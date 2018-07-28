#!/usr/bin/env kscript

@file:MavenRepository("imagej-releases","http://repo1.maven.org/maven2/")

//DEPS com.xenomachina:kotlin-argparser:2.0.6
//DEPS com.github.kittinunf.fuel:fuel:1.14.0
//DEPS com.beust:klaxon:3.0.1
//DEPS org.apache.poi:poi:3.17
//DEPS org.apache.poi:poi-ooxml:3.17

import com.beust.klaxon.Converter
import com.beust.klaxon.Json
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import com.xenomachina.argparser.ArgParser
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFDataFormat
import org.apache.poi.xssf.usermodel.XSSFRow
import java.io.InputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


fun JsonObject.uuid(fieldName: String): UUID? {
    val stringValue = string(fieldName) ?: return null
    try {
        return UUID.fromString(stringValue) ?: return null
    } catch (e: IllegalArgumentException) {
        return null
    }
}

fun Request.addCookie(cookie: String): Request = header(mapOf("cookie" to cookie))
fun Request.responseInputStream(): InputStream? = response().bodyInputStream()
fun Triple<Request, Response, Result<ByteArray, FuelError>>.bodyInputStream() = third.component1()?.inputStream()
fun Response.inputStream(): InputStream? = data.inputStream()


data class Project(
    val projectId: UUID,
    val name: String,
    val colorCode: ColorCode,
    @Json(name = "archived")
    val isArchived: Boolean,
    val changeToken: Long
) {

    enum class ColorCode {
        RED_1,
        BLUE_1,
        GREEN_1,
        ORANGE_1,
        PURPLE_1,
        TEAL_1
    }
}

data class User(
    val userId: UUID,
    val name: String,
    val emailId: String
)

class TimingClient(
    val baseURL: String,
    val teamName: String,
    val emailId: String,
    val password: String
) {

    private val authCookie: String
    private val userId: UUID
    private val organizationId: UUID

    private val uuidConverter = object: Converter {

        override fun canConvert(cls: Class<*>) = cls == UUID::class.java

        override fun toJson(value: Any): String = (value as UUID).toString()

        override fun fromJson(jv: JsonValue): Any {
            val stringValue = jv.string ?: throw KlaxonException("$this is not a String, and can't be parsed as a UUID")
            try {
                return UUID.fromString(stringValue)
            } catch (e: IllegalArgumentException) {
                throw KlaxonException("$stringValue is not a valid UUID, error: $e")
            }
        }

    }

    private val jsonParser = Parser()
    private val klaxon = Klaxon().converter(uuidConverter)
//    private val mapper = ObjectMapper().registerModule(KotlinModule())

    private fun Request.addAuthCookie(): Request = header(mapOf("cookie" to authCookie))

    init {
        val loginRequestBody = """
            {
                "teamName": "$teamName",
                "emailId": "$emailId",
                "password": "$password"
            }
        """.trimIndent()
        val loginResponse = Fuel.post("$baseURL/login").body(loginRequestBody).response()
        authCookie = loginResponse.second.headers["set-cookie"]!!.first()
        val loginResponseBody = jsonParser.parse(loginResponse.bodyInputStream()!!) as JsonObject
        userId = loginResponseBody.obj("user")!!.uuid("userId")!!
        organizationId = loginResponseBody.obj("user")!!.uuid("organizationId")!!
    }

    fun getProjects(): List<Project> {
        data class GetProjectsResponse(
            val projectsChangeToken: String,
            val projects: List<Project>
        )
        val res = Fuel.get("$baseURL/getProjects").addAuthCookie().responseInputStream()!!
        return klaxon.parse<GetProjectsResponse>(res)!!.projects
    }

    fun getUsers(): List<User> {
        data class GetUsersResponse(
            val users: List<User>
        )
        val res = Fuel.get("$baseURL/getUsers").addAuthCookie().responseInputStream()!!
        return klaxon.parse<GetUsersResponse>(res)!!.users
    }

    fun getReport(roundTo: String, userId: UUID, projectIds: List<UUID>, startDay: LocalDate, endDay: LocalDate): List<List<String?>> {
        val reqBody = """
            {
                "timeFormat": "HOURS",
                "levels": ["TIME_INTERVAL", "PROJECT", "USER"],
                "roundOff": {
                    "intervalInMinutes": 30,
                    "roundTo": "$roundTo"
                },
                "userIds": ["$userId"],
                "projectIds": [${projectIds.joinToString(", ") { "\"$it\"" }}],
                "duration": {
                    "granularity": "DAY",
                    "excludeDayOfWeek": true,
                    "timeZone": "+05:30",
                    "absolute": {
                        "startDay": "${startDay.format(DateTimeFormatter.ISO_DATE)}",
                        "endDay": "${endDay.format(DateTimeFormatter.ISO_DATE)}"
                    }
                }
            }
        """.trimIndent()
        val res = Fuel.post("$baseURL/generateReport").addAuthCookie().body(reqBody).responseInputStream()!!
        val resBody = jsonParser.parse(res) as JsonObject
        return resBody.array<JsonArray<String?>>("data")!!
    }
}


class Args(parser: ArgParser) {
    val startDay by parser.storing("-s", "--start-day", help="First day of report") { LocalDate.parse(this, DateTimeFormatter.ISO_DATE) }
    val endDay by parser.storing("-e", "--end-day", help="Last day of report") { LocalDate.parse(this, DateTimeFormatter.ISO_DATE) }
    val teamName by parser.storing("-t", "--team-name", help="Team name")
    val emailId by parser.storing("-E", "--email-id", help="Email Id")
    val password by parser.storing("-p", "--password", help="Password")
    val outputFile by parser.storing("-o", "--output-file", help="Path to output file") { File(this) }
    val adjustmentFactor by parser.storing("-a", "--adjustment-factor", help="Adjustment factor") { this.toDouble() }
}

val parsedArgs = ArgParser(args).parseInto(::Args)
val startDay = parsedArgs.startDay
val endDay = parsedArgs.endDay

val timingClient = TimingClient(
    baseURL = "https://api.usetiming.in/v1",
    teamName = parsedArgs.teamName,
    emailId = parsedArgs.emailId,
    password = parsedArgs.password
)

val projects = timingClient.getProjects().filter { !it.isArchived && it.name.toLowerCase().contains("leetsys") }
val users = timingClient.getUsers()

val sudipto = users.first { it.emailId.toLowerCase() == "sudipto.s@surya-soft.com" }
val nisarga = users.first { it.emailId.toLowerCase() == "nisarga.kh@surya-soft.com" }
val chetan = users.first { it.emailId.toLowerCase() == "chetan.m@surya-soft.com" }
val gopal = users.first { it.emailId.toLowerCase() == "gps@surya-soft.com" }

data class ReportLine(
    val day: LocalDate,
    val project: String,
    val hours: Double
)

fun createReport(lines: List<List<String?>>): List<ReportLine> {
    var lastDay: LocalDate = LocalDate.parse(lines[0][0], DateTimeFormatter.ofPattern("d/M/y"))
    val reportLines = mutableListOf<ReportLine>()
    for (line in lines) {
        if (line[0] != null) {
            lastDay = LocalDate.parse(line[0], DateTimeFormatter.ofPattern("d/M/y"))
            continue
        }
        if (line[1] != null) {
            reportLines.add(ReportLine(lastDay, line[1]!!, line[3]!!.toDouble()))
        }
    }
    return reportLines
}

val sudiptoReport = createReport(
    timingClient.getReport("NEAREST", sudipto.userId, projects.map { it.projectId }, startDay, endDay)
)
val nisargaReport = createReport(
    timingClient.getReport("NEAREST", nisarga.userId, projects.map { it.projectId }, startDay, endDay)
)
val chetanReport = createReport(
    timingClient.getReport("NEAREST", chetan.userId, projects.map { it.projectId }, startDay, endDay)
)
val gopalReport = createReport(
    timingClient.getReport("UP", gopal.userId, projects.map { it.projectId }, startDay, endDay)
)


val days = sortedSetOf<LocalDate>()
days.addAll(sudiptoReport.map { it.day })
days.addAll(nisargaReport.map { it.day })
days.addAll(chetanReport.map { it.day })
days.addAll(gopalReport.map { it.day })

val workBook = XSSFWorkbook()
val workSheet = workBook.createSheet()

val boldFont = workBook.createFont()
boldFont.bold = true
val boldStyle = workBook.createCellStyle()
boldStyle.setFont(boldFont)

val dateStyle = workBook.createCellStyle()
dateStyle.dataFormat = workBook.creationHelper.createDataFormat().getFormat("m/d/yy")

fun XSSFRow.newCell(columnIndex: Int, cellValue: String? = null, style: XSSFCellStyle? = null, cellType: CellType? = null): XSSFCell {
    val cell = createCell(columnIndex)
    if (style != null) {
        cell.cellStyle = style
    }
    if (cellValue != null) {
        cell.setCellValue(cellValue)
    }
    if (cellType != null) {
        cell.setCellType(cellType)
    }
    return cell
}

fun Double.roundToHalf(): Double = Math.round(this * 2) / 2.0

var rowNumber = 0

val headerRow = workSheet.createRow(rowNumber++)
headerRow.newCell(0, "Day", boldStyle)
headerRow.newCell(1, "Sprint", boldStyle)
headerRow.newCell(2, "Hours", boldStyle)
headerRow.newCell(3, "Gopal Hours", boldStyle)
headerRow.newCell(4, "Chetan Hours", boldStyle)
headerRow.newCell(5, "Sudipto Hours", boldStyle)
headerRow.newCell(6, "Nisarga Hours", boldStyle)
headerRow.newCell(7, "Sudipto Adjusted Hours", boldStyle)
headerRow.newCell(8, "Nisarga Adjusted Hours", boldStyle)

for (day in days) {
    val sprints = sortedSetOf<String>()
    sprints.addAll(sudiptoReport.filter { it.day == day }.map { it.project })
    sprints.addAll(nisargaReport.filter { it.day == day }.map { it.project })
    sprints.addAll(chetanReport.filter { it.day == day }.map { it.project })
    sprints.addAll(gopalReport.filter { it.day == day }.map { it.project })
    for (sprint in sprints) {
        val row = workSheet.createRow(rowNumber++)
        row.newCell(0, day.format(DateTimeFormatter.ofPattern("M/d/y")), dateStyle)
        row.newCell(1, sprint)
        val gopalHours = gopalReport.filter { it.day == day && it.project == sprint }.sumByDouble { it.hours }
        val chetanHours = chetanReport.filter { it.day == day && it.project == sprint }.sumByDouble { it.hours }
        var sudiptoHours = sudiptoReport.filter { it.day == day && it.project == sprint }.sumByDouble { it.hours }
        if (sudiptoHours > 8) sudiptoHours = 8.0
        var nisargaHours = nisargaReport.filter { it.day == day && it.project == sprint }.sumByDouble { it.hours }
        if (nisargaHours > 8) nisargaHours = 8.0
        val adjustedSudiptoHours = sudiptoHours / parsedArgs.adjustmentFactor
        val adjustedNisargaHours = nisargaHours / parsedArgs.adjustmentFactor
        val totalHours = (gopalHours + chetanHours + adjustedSudiptoHours + adjustedNisargaHours).roundToHalf()
        row.newCell(2, cellType = CellType.FORMULA).cellFormula = "ROUND((D$rowNumber+E$rowNumber+H$rowNumber+I$rowNumber)*2,0)/2"
        row.newCell(3, cellType = CellType.NUMERIC).setCellValue(gopalHours)
        row.newCell(4, cellType = CellType.NUMERIC).setCellValue(chetanHours)
        row.newCell(5, cellType = CellType.NUMERIC).setCellValue(sudiptoHours)
        row.newCell(6, cellType = CellType.NUMERIC).setCellValue(nisargaHours)
        row.newCell(7, cellType = CellType.FORMULA).cellFormula = "F$rowNumber/${parsedArgs.adjustmentFactor}"
        row.newCell(8, cellType = CellType.FORMULA).cellFormula = "G$rowNumber/${parsedArgs.adjustmentFactor}"
    }
}

rowNumber++
val totalRow = workSheet.createRow(rowNumber)
totalRow.newCell(0, "Total", style = boldStyle)
totalRow.newCell(2, style = boldStyle, cellType = CellType.FORMULA).cellFormula = "SUM(C2:C$rowNumber)"

for (col in 0..8) {
    workSheet.autoSizeColumn(col)
}

FileOutputStream(parsedArgs.outputFile).use { fos ->
    workBook.write(fos)
}
