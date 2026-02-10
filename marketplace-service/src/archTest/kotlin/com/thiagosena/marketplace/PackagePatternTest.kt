package com.thiagosena.marketplace

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import kotlin.test.Test

class PackagePatternTest {

    private val basePackage = "com.thiagosena.marketplace"
    private val importedClassesApplication = ClassFileImporter().importPackages("$basePackage.application")
    private val importedClassesDomain = ClassFileImporter().importPackages("$basePackage.domain")
    private val importedClassesResources = ClassFileImporter().importPackages("$basePackage.resources")

    @Test
    fun `classes that ends with Controller shoud be in a controller package inside web`() {
        ArchRuleDefinition.classes().that().haveSimpleNameEndingWith("Controller").should()
            .resideInAPackage("..web..").allowEmptyShould(true).check(importedClassesApplication)
    }

    @Test
    fun `classes that ends with Config shoud be in a config package inside web`() {
        ArchRuleDefinition.classes().that().haveSimpleNameEndingWith("Configuration").should()
            .resideInAPackage("..config..").allowEmptyShould(true).check(importedClassesApplication)
    }

    @Test
    fun `classes that ends with Service shoud be in a service package inside domain`() {
        ArchRuleDefinition.classes().that().haveSimpleNameEndingWith("Service").should()
            .resideInAPackage("..services..").allowEmptyShould(true).check(importedClassesDomain)
    }

    @Test
    fun `classes that ends with Exception shoud be in a exceptions package`() {
        ArchRuleDefinition.classes().that().haveSimpleNameEndingWith("Exception").should()
            .resideInAPackage("..exceptions..").allowEmptyShould(true).let {
                it.check(importedClassesApplication)
                it.check(importedClassesDomain)
                it.check(importedClassesResources)
            }
    }

    @Test
    fun `classes that ends with Gateway shoud be in a gateways package inside domain`() {
        ArchRuleDefinition.classes().that().haveSimpleNameEndingWith("Gateway").should()
            .resideInAPackage("..gateways..").allowEmptyShould(true).check(importedClassesResources)
    }
}