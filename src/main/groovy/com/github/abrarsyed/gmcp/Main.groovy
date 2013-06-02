package com.github.abrarsyed.gmcp

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

import net.md_5.specialsource.AccessMap
import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.RemapperPreprocessor

import com.github.abrarsyed.gmcp.Constants.OperatingSystem
import com.google.common.io.Files

import difflib.DiffUtils

class Main
{
	public static OperatingSystem os = OperatingSystem.WINDOWS

	// to be made configureable by gradle
	public static String MC_VERSION = "1.5.2"
	public static String FORGE_VERSION = "7.8.0.710"

	public static void main(args)
	{
		os = Util.getOS()
		
		println "cleaning up past builds...."
		Util.createOrCleanDir(Constants.DIR_TEMP)
		Util.createOrCleanDir(Constants.DIR_LOGS)

		println "DOWNLOADING STUFF !!!!!!!!!!!!"

		downloadStuff()

		println "MERGING JARS !!!!!!!!!!!!"

		mergeJars(Constants.JAR_CLIENT, Constants.JAR_SERVER)

		println "DeObfuscating With SpecialSource !!!!!!!!!!!!"

		deobfuscate(Constants.JAR_MERGED, Constants.JAR_DEOBF)

		println "Applying Exceptor (MCInjector) !!!!!!!!!!!!"

		inject()

		println "UNZIPPING !!!!!!!!!!!!"

		Util.unzip(Constants.JAR_EXCEPTOR, Constants.DIR_EXTRACTED, false)

		println "COPYING CLASSES!!!!!!!"

		copyClasses(Constants.DIR_EXTRACTED, Constants.DIR_CLASSES)

		println "DECOMPILING !!!!!!!!!!!!"

		decompile()

		println "APPLY FF FIXES!!!!!!!"

		FFPatcher.processDir(Constants.DIR_SOURCES)

		println "APPLYING MCP PATCHES!!!!!!!"

		patchMCP()

		println "REMAPPING SOURCES AND INJECTING JAVADOCS!!!!!!!!"

		renameSources(Constants.DIR_SOURCES)
		
		println "FORMATTING SOURCES!!!!!!!!"
		
		formatSources(Constants.DIR_SOURCES)
		
		println "APPLYING FORGE AND FML PATCHES!!!!!!!!"
		
		applyPatches(Constants.DIR_FML_PATCHES, Constants.DIR_SOURCES)
		applyPatches(Constants.DIR_FORGE_PATCHES, Constants.DIR_SOURCES)
		
		println "COMPLETE!"
	}

	def static mergeJars(File client, File server)
	{
		JarBouncer.MCPMerger(client, server, new File(Constants.DIR_FML, "mcp_merge.cfg"))

		// copy and strip META-INF
		def output = new ZipOutputStream(Constants.JAR_MERGED.newDataOutputStream());
		def ZipFile input = new ZipFile(Constants.JAR_CLIENT)
		
		input.entries().each{ ZipEntry it ->
			if (it.name.contains("META-INF"))
				return
			else if (it.size > 0)
			{
				output.putNextEntry(it)
				output.write(input.getInputStream(it).bytes)
				output.closeEntry();
			}
		}
		
		input.close()
		output.close()
	}

	def static decompile()
	{
		Constants.DIR_SOURCES.mkdirs()
		JarBouncer.fernFlower(Constants.DIR_CLASSES.getPath(), Constants.DIR_SOURCES.getPath())
	}

	def static deobfuscate(File inJar, File outJar)
	{
		// load mapping
		JarMapping mapping = new JarMapping()
		mapping.loadMappings(new File(Constants.DIR_MAPPINGS, "joined.srg"))

		// load in AT
		def accessMap = new AccessMap()
		accessMap.loadAccessTransformer(new File(Constants.DIR_FML, "common/fml_at.cfg"))
		accessMap.loadAccessTransformer(new File(Constants.DIR_FORGE, "common/forge_at.cfg"))
		def processor = new  RemapperPreprocessor(null, mapping, accessMap)

		// make remapper
		JarRemapper remapper = new JarRemapper(processor, mapping)

		// load jar
		Jar input = Jar.init(inJar)

		// remap jar
		remapper.remapJar(input, outJar)
	}

	def static inject()
	{
		JarBouncer.injector(Constants.JAR_DEOBF, Constants.JAR_EXCEPTOR, new File(Constants.DIR_MAPPINGS, "joined.exc"))
	}

	def static copyClasses(File inDir, File outDir)
	{
		outDir.mkdirs()

		inDir.eachFileRecurse
		{
			// check ignored packages....
			if (!it.path.contains("net\\minecraft"))
			{
				return
			}

			def out = new File(it.getAbsolutePath().replace(inDir.absolutePath, outDir.absolutePath))
			if (it.isFile() && Files.getFileExtension(it.getPath()) == "class")
			{
				out.createNewFile()
				Files.copy(it, out)
			}
			else if (it.isDirectory())
			{
				out.mkdirs()
			}
		}
	}

	def static patchMCP()
	{
		// have to generate diffs... maybe...
		def rawPatch = new File(Constants.DIR_MCP_PATCHES, "minecraft_ff.patch").text.readLines()

		def patchMap = [:]
		def patternDiff = /diff.*?minecraft\\(.+?) .*?/
		def patternStart = /^\+\+\+/

		def currentFile, startIndex = 0, endIndex = 0
		rawPatch.eachWithIndex
		{ obj, int i ->
			def matcher = obj =~ patternDiff
			if (matcher)
			{
				currentFile = matcher[0][1]
				endIndex = i-1
				if (endIndex > 0)
				{
					patchMap[currentFile] = DiffUtils.parseUnifiedDiff(rawPatch.subList(startIndex, endIndex))
					endIndex = 0
				}
				return
			}

			matcher = obj =~ patternStart
			if (matcher)
			{
				startIndex = i+1
			}
		}

		println "seems to have loaded patches"

		def currentLines, newLines, text, file
		patchMap.each
		{
			println "writing for "+it.getKey()
			file = new File(Constants.DIR_SOURCES, it.getKey())
			currentLines = file.text.readLines()
			newLines = DiffUtils.patch(currentLines, it.getValue())
			text = newLines.join(System.lineSeparator)
			file.write(text)
		}

		println "seems to have patched the lines now."
	}
	
	def static applyPatches(File from, File to)
	{
		def patchMap = [:]
		def newFile, patch; 
		
		// recurse through files
		from.eachFileRecurse {
			// if its a patch
			if (it.isFile() && it.path.endsWith(".patch"))
			{
				newFile = new File(to, Util.getRelative(from, it).replace(/.patch/, ""))
				patch = DiffUtils.parseUnifiedDiff(it.text.readLines())
				patchMap.put(newFile, patch)
			}
		}
		
		def currentLines, newLines, text, file
		patchMap.each
		{
			currentLines = it.getKey().text.readLines()
			newLines = DiffUtils.patch(currentLines, it.getValue())
			text = newLines.join(System.lineSeparator)
			it.getKey().write(newLines)
		}
	}

	def static downloadStuff()
	{
		def root = Constants.DIR_MC_JARS
		if (!root.exists() || !root.isDirectory())
			root.mkdirs()

		println "Downloading Minecraft"
		def mcver = MC_VERSION.replaceAll(/\./, /_/)
		Util.download(String.format(Constants.URL_MC_JAR, mcver), Constants.JAR_CLIENT)
		Util.download(String.format(Constants.URL_MCSERVER_JAR, mcver), Constants.JAR_SERVER)

		println "Downloading libraries"
		Constants.LIBRARIES.each {
			Util.download(Constants.URL_LIB_ROOT + it, new File(root, it))
		}

		println "Downloading natives"
		def nativesJar = new File(Constants.DIR_TEMP, "natives.jar")
		Util.download(Constants.URL_LIB_ROOT+Constants.NATIVES[os.toString()], nativesJar)

		Util.unzip(nativesJar, new File(root, "natives"), true)
		nativesJar.delete()

		println "Downloading Forge"
		def forge = new File(Constants.DIR_TEMP, "forge.zip")
		Util.download(String.format(Constants.URL_FORGE, MC_VERSION, FORGE_VERSION), forge)
		Util.unzip(forge, Constants.DIR_TEMP, true)
		forge.delete()
	}

	def static renameSources(File dir)
	{
		def files = [:]
		
		Constants.CSVS.entrySet().every {
			files[it.key] = new File(Constants.DIR_MAPPINGS, it.value)
		}

		SourceRemapper remapper = new SourceRemapper(files)

		dir.eachFileRecurse {
			if (it.isFile())
				remapper.remapFile(dir, it)
		}
		
	}
	
	def static formatSources(File dir)
	{
		JarBouncer.formatter(dir, new File(Constants.DIR_MAPPINGS, "astyle.cfg"))
	}
}
