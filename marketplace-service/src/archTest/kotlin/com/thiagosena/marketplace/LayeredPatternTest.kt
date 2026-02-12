package com.thiagosena.marketplace

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import kotlin.test.Test

class LayeredPatternTest {
    companion object {
        const val APP = "application"
        const val DOMAIN = "domain"
        const val RESOURCES = "resources"
    }

    private val classes = ClassFileImporter().importPackages("com.thiagosena.marketplace")

    @Test
    fun `should respect layered architecture`() {
        layeredArchitecture()
            .consideringAllDependencies()
            .layer(APP)
            .definedBy("..$APP..")
            .layer(DOMAIN)
            .definedBy("..$DOMAIN..")
            .layer(RESOURCES)
            .definedBy("..$RESOURCES..")
            .whereLayer(APP)
            .mayOnlyBeAccessedByLayers(RESOURCES)
            .whereLayer(DOMAIN)
            .mayOnlyBeAccessedByLayers(APP, RESOURCES)
            .whereLayer(RESOURCES)
            .mayNotBeAccessedByAnyLayer()
            .check(classes)
    }
}
