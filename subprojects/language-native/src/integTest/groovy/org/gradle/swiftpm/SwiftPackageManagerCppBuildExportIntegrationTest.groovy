/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.swiftpm

import org.gradle.nativeplatform.fixtures.app.CppAppWithLibraries
import org.gradle.nativeplatform.fixtures.app.CppLib

class SwiftPackageManagerCppBuildExportIntegrationTest extends AbstractSwiftPackageManagerExportIntegrationTest {
    def "produces manifest for single project C++ library"() {
        given:
        buildFile << """
            plugins { 
                id 'swiftpm-export' 
                id 'cpp-library'
            }
"""
        def lib = new CppLib()
        lib.sources.writeToProject(testDirectory)
        lib.privateHeaders.writeToSourceDir(testDirectory.file("src/main/cpp"))
        lib.publicHeaders.writeToProject(testDirectory)

        when:
        run("generateSwiftPmManifest")

        then:
        file("Package.swift").text == """// swift-tools-version:4.0
//
// GENERATED FILE - do not edit
//
import PackageDescription

let package = Package(
    name: "test",
    products: [
        .library(name: "test", targets: ["test"]),
    ],
    targets: [
        .target(
            name: "test",
            path: ".",
            sources: [
                "src/main/cpp/greeter.cpp",
                "src/main/cpp/sum.cpp",
            ],
            publicHeadersPath: "src/main/public"
        ),
    ]
)
"""
        swiftPmBuildSucceeds()
    }

    def "produces manifest for multi project C++ build"() {
        given:
        settingsFile << "include 'lib1', 'lib2'"
        buildFile << """
            plugins { 
                id 'swiftpm-export' 
                id 'cpp-application' 
            }
            subprojects {
                apply plugin: 'cpp-library'
            }
            dependencies {
                implementation project(':lib1')
            }
            project(':lib1') {
                dependencies {
                    implementation project(':lib2')
                }
            }
"""
        def app = new CppAppWithLibraries()
        app.main.writeToProject(testDirectory)
        app.greeterLib.sources.writeToProject(file("lib1"))
        app.greeterLib.publicHeaders.writeToProject(file("lib1"))
        app.greeterLib.privateHeaders.writeToSourceDir(file("lib1/src/main/cpp"))
        app.loggerLib.writeToProject(file("lib2"))

        when:
        run("generateSwiftPmManifest")

        then:
        file("Package.swift").text == """// swift-tools-version:4.0
//
// GENERATED FILE - do not edit
//
import PackageDescription

let package = Package(
    name: "test",
    products: [
        .executable(name: "test", targets: ["test"]),
        .library(name: "lib1", targets: ["lib1"]),
        .library(name: "lib2", targets: ["lib2"]),
    ],
    targets: [
        .target(
            name: "test",
            dependencies: [
                .target(name: "lib1"),
            ],
            path: ".",
            sources: [
                "src/main/cpp/main.cpp",
            ]
        ),
        .target(
            name: "lib1",
            dependencies: [
                .target(name: "lib2"),
            ],
            path: "lib1",
            sources: [
                "src/main/cpp/greeter.cpp",
            ],
            publicHeadersPath: "src/main/public"
        ),
        .target(
            name: "lib2",
            path: "lib2",
            sources: [
                "src/main/cpp/logger.cpp",
            ],
            publicHeadersPath: "src/main/public"
        ),
    ]
)
"""
        swiftPmBuildSucceeds()
    }
}
