package com.kelvinsaputra.promptstudio

import com.kelvinsaputra.promptstudio.domain.*
import com.kelvinsaputra.promptstudio.persistence.*
import com.kelvinsaputra.promptstudio.prompt.PromptCompiler
import kotlinx.serialization.json.*
import kotlin.test.*

class ProjectJsonTest {
    private val project = CharacterProject(
        subject = "An original adult explorer.",
        costume = CostumeConfiguration(silhouette = null, footwear = Footwear.TABI_INSPIRED_BOOTS,
            customization = setOf(Customization.CHARM, Customization.PATCHES), customNotes = "藍色\n\"Canvas\" 🧵"),
        pose = PoseConfiguration(basePose = BasePose.PERCHED, gaze = null, customNotes = "Hand on ledge"),
        output = OutputConfiguration(type = OutputType.CUSTOM, customAspectRatio = "2.35:1", customIntent = "panorama", clockSafe = false),
    )

    @Test fun meaningfulConfigurationRoundTripsWithoutPromptChanges() {
        val encoded = ProjectJson.encode(project)
        val decoded = ProjectJson.decode(encoded)
        assertEquals(project, decoded)
        assertEquals(PromptCompiler().compile(project), PromptCompiler().compile(decoded))
        assertEquals("藍色\n\"Canvas\" 🧵", decoded.costume.customNotes)
        assertContains(encoded, "\n    \"version\": 1")
        assertContains(encoded, "TABI_INSPIRED_BOOTS")
    }

    @Test fun defaultProjectIncludesExplicitVersionAndExpandedSections() {
        val root = Json.parseToJsonElement(ProjectJson.encode(CharacterProject())).jsonObject
        assertEquals(JsonPrimitive(1), root["version"])
        assertTrue(root.keys.containsAll(listOf("version", "id", "name", "style", "subject", "identity", "face", "powerSignature", "costume", "pose", "output", "priorityStack")))
        assertEquals(CharacterProject(), ProjectJson.decode(root.toString()))
    }

    @Test fun malformedMissingAndIncompatibleFilesAreRejected() {
        for (text in listOf("{broken", "null", "[]", "{}", "{\"version\":1}", "{\"version\":\"1\"}")) {
            assertFailsWith<ProjectFileException>(text) { ProjectJson.decode(text) }
        }
        val future = ProjectJson.encode(project).replace("\"version\": 1", "\"version\": 2")
        assertContains(assertFailsWith<ProjectFileException> { ProjectJson.decode(future) }.message.orEmpty(), "Unsupported project version 2")
    }

    @Test fun invalidEnumAndDomainConstraintsAreRejected() {
        val root = Json.parseToJsonElement(ProjectJson.encode(project)).jsonObject
        val badEnum = ProjectJson.encode(project).replace("TABI_INSPIRED_BOOTS", "SPACE_BOOTS")
        assertFailsWith<ProjectFileException> { ProjectJson.decode(badEnum) }
        val costume = JsonObject(root.getValue("costume").jsonObject + ("customization" to JsonArray(listOf("PATCHES", "CHARM", "PINS").map(::JsonPrimitive))))
        assertFailsWith<ProjectFileException> { ProjectJson.decode(JsonObject(root + ("costume" to costume)).toString()) }
    }

    @Test fun oversizedProjectIsRejectedOnBothImportAndExport() {
        val huge = "x".repeat(ProjectJson.MAX_BYTES + 1)
        assertFailsWith<ProjectFileException> { ProjectJson.decode(huge) }
        assertFailsWith<ProjectFileException> { ProjectJson.encode(project.copy(subject = huge)) }
    }
}
