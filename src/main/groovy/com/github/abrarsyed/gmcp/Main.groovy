package com.github.abrarsyed.gmcp

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

import name.fraser.neil.plaintext.DiffMatchPatch
import net.md_5.specialsource.AccessMap
import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.RemapperPreprocessor
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider

import com.github.abrarsyed.gmcp.Constants.OperatingSystem
import com.github.abrarsyed.gmcp.source.FFPatcher
import com.github.abrarsyed.gmcp.source.MCPCleanup
import com.github.abrarsyed.gmcp.source.SourceRemapper
import com.google.common.io.Files

import difflib.DiffUtils
import difflib.Patch

class Main
{
	public static OperatingSystem os = OperatingSystem.WINDOWS

	// to be made configureable by gradle
	public static String MC_VERSION = "1.5.2"
	public static String FORGE_VERSION = "7.8.0.710"

	public static DiffMatchPatch patcher = new DiffMatchPatch()

	public static void main(args)
	{
		os = Util.getOS()
		patcher.Match_Distance = 0000
		patcher.Match_Threshold = 1
		patcher.Patch_DeleteThreshold = 1
		patcher.Patch_Margin = 100

		println "cleaning up past builds...."
		Util.createOrCleanDir(Constants.DIR_TEMP)
		Util.createOrCleanDir(Constants.DIR_LOGS)

		println "DOWNLOADING STUFF !!!!!!!!!!!!"

		downloadStuff()

		println "MERGING JARS !!!!!!!!!!!!"

		mergeJars(Constants.JAR_CLIENT, Constants.JAR_SERVER)

		println "FIXING PACKAGES!!!!!!!!"

		// creates the package fixer
		(new PackageFixer(new File(Constants.DIR_MAPPINGS, Constants.CSVS["packages"]))).with {
			// calls the following on the package fixer.
			// gotta love groovy :)
			fixSRG(new File(Constants.DIR_MAPPINGS, "joined.srg"), new File(Constants.DIR_MAPPINGS, "packaged.srg"))
			fixExceptor(new File(Constants.DIR_MAPPINGS, "joined.exc"), new File(Constants.DIR_MAPPINGS, "packaged.exc"))
			fixPatch(new File(Constants.DIR_MCP_PATCHES, "minecraft_ff.patch"))
			fixPatch(new File(Constants.DIR_MCP_PATCHES, "minecraft_server_ff.patch"))
		}

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
		//
		//		println "FORMATTING SOURCES!!!!!!!!"
		//
		//		formatSources(Constants.DIR_SOURCES)
		//
		//		println "DOING FML FIXES!"
		//
		//		Constants.DIR_SOURCES.eachFileRecurse {
		//			if (it.isFile())
		//				FMLCleanup.updateFile(it)
		//		}
		//
		//		println "APPLYING FML PATCHES =================================================="
		//
		//		applyPatches(Constants.DIR_FML_PATCHES, Constants.DIR_SOURCES)
		//
		//		println "RENAMING SOURCES TO NICE NAMES =================================================="
		//
		//		renameSources(Constants.DIR_SOURCES)
		//
		//
		//
		//		println "APPLYING FORGE PATCHES =================================================="
		//
		//		applyPatches(Constants.DIR_FORGE_PATCHES, Constants.DIR_SOURCES)

		println "COMPLETE!"
	}

	def static mergeJars(File client, File server)
	{
		JarBouncer.MCPMerger(client, server, new File(Constants.DIR_FML, "mcp_merge.cfg"))

		// copy and strip META-INF
		def output = new ZipOutputStream(Constants.JAR_MERGED.newDataOutputStream())
		def ZipFile input = new ZipFile(Constants.JAR_CLIENT)

		input.entries().each{ ZipEntry it ->
			if (it.name.contains("META-INF"))
				return
			else if (it.size > 0)
			{
				output.putNextEntry(it)
				output.write(input.getInputStream(it).bytes)
				output.closeEntry()
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
		mapping.loadMappings(new File(Constants.DIR_MAPPINGS, "packaged.srg"))

		// load in AT
		def accessMap = new AccessMap()
		accessMap.loadAccessTransformer(new File(Constants.DIR_FML, "common/fml_at.cfg"))
		accessMap.loadAccessTransformer(new File(Constants.DIR_FORGE, "common/forge_at.cfg"))
		def processor = new  RemapperPreprocessor(null, mapping, accessMap)

		// make remapper
		JarRemapper remapper = new JarRemapper(processor, mapping)

		// load jar
		Jar input = Jar.init(inJar)

		// ensure that inheritance provider is used
		JointProvider inheritanceProviders = new JointProvider()
		inheritanceProviders.add(new JarProvider(input))
		mapping.setFallbackInheritanceProvider(inheritanceProviders)

		// remap jar
		remapper.remapJar(input, outJar)
	}

	def static inject()
	{
		JarBouncer.injector(Constants.JAR_DEOBF, Constants.JAR_EXCEPTOR, new File(Constants.DIR_MAPPINGS, "packaged.exc"))
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
		def patternDiff = /^diff.*?minecraft\\(.+?) .*?/
		def startPattern = /^[+]{3}.*/

		def currentFile, startIndex = 0, endIndex = 0
		rawPatch.eachWithIndex
		{ obj, int i ->
			def matcher = obj =~ patternDiff
			if (matcher)
			{
				// check first null
				if (currentFile == null)
				{
					currentFile = matcher[0][1]
					//startIndex = i+1
				}

				endIndex = i-1
				if (endIndex > 0)
				{
					def patchlist = rawPatch.subList(startIndex, endIndex)
					//patchMap[currentFile] = DiffUtils.parseUnifiedDiff(patchlist)
					patchMap[currentFile] = patcher.patch_fromText(patchlist.join("\n"))

					// for next one
					currentFile = matcher[0][1]
					//startIndex = i+1
					endIndex = 0
				}
				return
			}

			if (obj ==~ startPattern)
				startIndex = i+1
		}

		print ""

		def counter = 0, success = 0
		patchMap.each
		{ file, delta ->
			file = new File(Constants.DIR_SOURCES, file)
			//def lines = file.text.readLines()
			def result = patcher.patch_apply(delta, file.text)
			def newText = result[0]
			def failed = false
			result[1].each {
				println "$file >> "+it
				if (!it)
					failed = true
			}
			//lines = delta.applyTo(lines)
			//file.write(lines.join("\n"))
			file.write(newText)
			if (!failed)
				success++
			counter++
		}

		println success+" out of "+counter + " succeeded"
	}

	def static applyPatches(File from, File to)
	{
		Map<File, Patch> patchMap = [:]
		def newFile, patch

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

		def counter = 0, success = 0
		patchMap.each
		{ file, delta ->
			try
			{
				def lines = file.text.readLines()
				lines = delta.applyTo(lines)
				file.write(lines.join("\n"))
				success++
			}
			catch(Exception e)
			{
				println "error patching "+file+"   skipping."
				if (counter <= 1)
					e.printStackTrace()
			}
			counter++
		}

		println success + " out of " + counter + " succeeded"
	}

	def static downloadStuff()
	{
		def root = Constants.DIR_MC_JARS
		if (!root.exists() || !root.isDirectory())
			root.mkdirs()

		// TODO: Downlaod forge, and then read forge configs for it.

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
		//Util.download(ForgeVersionGetter.getUrl(MC_VERSION), forge)
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
				remapper.remapFile(it)
		}

	}

	def static formatSources(File dir)
	{
		// do cleanup
		MCPCleanup.cleanDir(dir)

		JarBouncer.formatter(dir, new File(Constants.DIR_MAPPINGS, "astyle.cfg"))
		//JarBouncer.formatter(dir, new File("formatter.cfg"))
	}
}
