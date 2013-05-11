package com.github.abrarsyed.gmcp

import au.com.bytecode.opencsv.CSVReader

import com.google.common.io.Files

class Main
{
	public static final File tmp = new File("tmp");
	public static final File resources = new File("resources");
	public static final File logs = new File(tmp, "logs");

	public static final File extracted = new File(tmp, "extracted")
	public static final File classes = new File(tmp, "classes")
	public static final File sources = new File(tmp, "sources")
	public static final File SS_JAR = new File(tmp, "Minecraft_SS.jar")
	public static final File EXC_JAR = new File(tmp, "Minecraft_EXC.jar")

	public static final File JAR = new File(tmp, "jars/Minecraft.jar")

	public static void main(args)
	{
		logs.mkdirs()
		tmp.mkdirs()
		resources.mkdirs()

		downloadStuff()

		println "DeObfuscating With SpecialSource !!!!!!!!!!!!"

		deobfuscate()

		println "Applying Exceptor (MCInjector) !!!!!!!!!!!!"

		inject()

		println "UNZIPPING !!!!!!!!!!!!"

		Util.unzip(EXC_JAR, extracted, false)

		println "COPYING CLASSES!!!!!!!"

		copyClasses(extracted, classes)

		println "DECOMPILING !!!!!!!!!!!!"

		decompile()

		println "APPLY FF PATCHES!!!!!!!"

		FFPatcher.processDir(sources)

		println "COMPLETE!"
	}

	def static decompile()
	{
		sources.mkdirs();
		JarBouncer.fernFlower(classes.getPath(), sources.getPath());
	}

	def static deobfuscate()
	{
		JarBouncer.specialSource(JAR, SS_JAR, new File(resources, "srgs/client.srg"));
	}

	def static inject()
	{
		JarBouncer.injector(SS_JAR, EXC_JAR, new File(resources, "joined.exc"))
	}

	def static copyClasses(File inDir, File outDir)
	{
		outDir.mkdirs();

		inDir.eachFileRecurse
		{
			// check ignored packages....
			if (isIgnored(it.getPath()))
			{
				return;
			}

			def out = new File(it.getAbsolutePath().replace(inDir.absolutePath, outDir.absolutePath))
			if (it.isFile() && Files.getFileExtension(it.getPath()) == "class")
			{
				out.createNewFile();
				Files.copy(it, out)
			}
			else if (it.isDirectory())
			{
				out.mkdirs();
			}
		}
	}

	def static boolean isIgnored(String str)
	{
		switch(str)
		{
			case ~".*?paulscode.*": return true
			case ~".*?com/jcraft.*": return true
			case ~".*?isom.*": return true
			case ~".*?ibxm.*": return true
			case ~".*?de/matthiasmann/twl.*": return true
			case ~".*?org/xmlpull.*": return true
			case ~".*?javax/xml.*": return true
			case ~".*?com/fasterxml.*": return true
			case ~".*?javax/ws.*": return true
			default: return false
		}
	}

	def static readCSV()
	{
		def reader = new CSVReader();
		def elements = reader.readAll();

		elements.each
		{
			it.each
			{ println it }
		}
	}

	def static downloadStuff()
	{
		def root = new File(tmp, "jars")
		if (!root.exists() || !root.isDirectory())
			root.mkdirs()

		ConfigParser parser = new ConfigParser(resources.path+"/mc_versions.cfg")

		def version = parser.getProperty("default", "current_ver")
		println "downloading Minecraft"
		Util.download(parser.getProperty(version, "client_url"), JAR)

		def dls = parser.getProperty("default", "libraries").split(/\s/)
		def url = parser.getProperty("default", "base_url")
		println "downloading libraries"
		dls.each
		{
			Util.download(url+it, new File(root, it))
		}

		def operating = Util.getOS();

		println "downlaoding natives"
		dls = parser.getProperty("default", "natives").split(/\s/)[operating.ordinal()]
		File nDL = new File(tmp, dls);
		Util.download(url+dls, nDL)

		Util.unzip(nDL, new File(root, "natives"), true)
		nDL.delete()
	}
}
