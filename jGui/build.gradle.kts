import gg.essential.gradle.util.*

plugins {
    id("gg.essential.multi-version")
    id("gg.essential.defaults.repo")
    id("gg.essential.defaults.java")
    id("gg.essential.defaults.loom")
}

// Force Java 21 for all versions (only JDK available on this machine)
java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

if (platform.isNeoForge && platform.mcVersion == 12101) {
    configurations.named("mappings") { dependencies.clear() }
    dependencies {
        mappings(loom.layered {
            mappings(project.property("essential.defaults.loom.mappings").toString())
            mappings("dev.architectury:yarn-mappings-patch-neoforge:1.21+build.6")
        })
    }
}

loom {
    noRunConfigs()
}

if (!platform.isUnobfuscated) {
    loom.mixin.useLegacyMixinAp = true
    loom.mixin.defaultRefmapName.set("mixins.jgui.refmap.json")
}

java.withSourcesJar()

repositories {
    mavenLocal()
    exclusiveContent {
        forRepository { maven("https://maven.fabricmc.net") }
        filter { includeModule("net.fabricmc", "yarn") }
    }
    maven("https://jitpack.io") {
        content {
            includeGroupByRegex("com\\.github\\..*")
        }
    }
    maven("https://repo.spongepowered.org/maven/") // for 0.7.11-SNAPSHOT Mixin
}

dependencies {
    api("com.github.ReplayMod:lwjgl-utils:27dcd66")

    if (!platform.isFabric) {
        // Mixin 0.8 is no longer compatible with MC 1.11.2 or older
        val mixinVersion = if (platform.isNeoForge) "0.8.7" else if (platform.mcVersion >= 11200) "0.8.2" else "0.7.11-SNAPSHOT"
        compileOnly("org.spongepowered:mixin:$mixinVersion")
    }

    if (platform.mcVersion >= 11604) {
        if (platform.isNeoForge) {
            // Match ReplayMod's annotations; the loader supplies the runtime implementation.
            compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.6")!!)
        } else {
            implementation(annotationProcessor("com.github.LlamaLad7:MixinExtras:0.1.1")!!)
        }
    }
}
