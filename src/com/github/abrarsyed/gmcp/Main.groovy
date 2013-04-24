package com.github.abrarsyed.gmcp

class Main
{
	public static void main(args)
	{
		//downloadStuff()

		println "DeObfuscating With Rettrogaurd !!!!!!!!!!!!"

		deobfuscate()
		
		println "UNZIPPING !!!!!!!!!!!!"
		
		Util.unzip("tmp/bin/Minecraft.jar", "tmp/extracted", false)
		
		println "DECOMPILING !!!!!!!!!!!!"

		decompile()

		println "COMPLETE!"
	}

	def static decompile()
	{
		new File("tmp/decompiled").mkdirs();
		JarBouncer.fernFlower("rmp/extracted", "tmp/decompiled");
	}

	def static deobfuscate()
	{
		def cp = new StringBuilder();
		cp.append("tmp/bin/minecraft.jar").append(',')
		cp.append("tmp/bin/jinput.jar").append(',')
		cp.append("tmp/bin/lwjgl.jar").append(',')
		cp.append("tmp/bin/lwjgl_util.jar")

		// %(DirJars)s/bin/minecraft.jar,%(DirJars)s/bin/jinput.jar,%(DirJars)s/bin/lwjgl.jar,%(DirJars)s/bin/lwjgl_util.jar
		JarBouncer.retroGuardDeObf(cp.toString(), "tmp/SRGs/client_rg.cfg");
	}

	def static downloadStuff()
	{
		def root = new File("tmp/bin")
		if (!root.exists() || !root.isDirectory())
			root.mkdirs()

		ConfigParser parser = new ConfigParser("mc_versions.cfg")

		def version = parser.getProperty("default", "current_ver")
		println "downloading Minecraft"
		Util.download(parser.getProperty(version, "client_url"), root.path+"/"+"minecraft.jar")

		def dls = parser.getProperty("default", "libraries").split(/\s/)
		def url = parser.getProperty("default", "base_url")
		println "downloading libraries"
		dls.each
		{
			Util.download(url+it, root.path+"/"+it)
		}

		def operating = Util.getOS();

		println "downlaoding natives"
		def natives = parser.getProperty("default", "natives").split(/\s/)[operating.ordinal()]
		Util.download(url+natives, "tmp/"+natives)

		def nDir = new File("tmp/bin/natives")
		if (!nDir.exists() || !nDir.isDirectory())
			nDir.mkdirs()

		Util.unzip("tmp/"+natives, nDir.path, true)
		new File("tmp/"+natives).delete()
	}
}
