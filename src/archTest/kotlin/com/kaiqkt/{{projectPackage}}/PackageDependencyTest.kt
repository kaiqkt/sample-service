package com.kaiqkt.`{{projectPackage}}`

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.jupiter.api.Test

class PackageDependencyTest {
    private val importedClassesRoot = ClassFileImporter().importPackages("com.kaiqkt")

    @Test
    fun `application should not depend of resources package`() {
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..resources..")
            .check(importedClassesRoot)
    }

    @Test
    fun `resources should not depend of application package`() {
        noClasses().that().resideInAPackage("..resources..")
            .should().dependOnClassesThat().resideInAPackage("..application..")
            .check(importedClassesRoot)
    }

    @Test
    fun `domain should not depend of resources or application packages`() {
        noClasses().that().resideInAPackage("..resources..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..resources..")
            .check(importedClassesRoot)
    }

    @Test
    fun `layers of the application should be respected`() {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("application").definedBy("..application..")
            .layer("domain").definedBy("..domain..")
            .layer("resources").definedBy("..resources..")
            .whereLayer("application").mayNotBeAccessedByAnyLayer()
            .whereLayer("domain").mayOnlyBeAccessedByLayers("application", "domain")
            .whereLayer("resources").mayOnlyBeAccessedByLayers("domain")
    }
}