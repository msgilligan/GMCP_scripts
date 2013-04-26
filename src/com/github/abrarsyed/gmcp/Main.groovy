package com.github.abrarsyed.gmcp

class Main
{
	public static final File tmp = new File("tmp");
	public static final File resources = new File("resources");
	
	public static void main(args)
	{
		downloadStuff()

		println "DeObfuscating With SpecialSource !!!!!!!!!!!!"

		deobfuscate()
		
		println "UNZIPPING !!!!!!!!!!!!"
		
		Util.unzip(new File(tmp, "Minecraft_ss.jar"), new File(tmp, "extracted"), false)
		
		println "DECOMPILING !!!!!!!!!!!!"

		decompile()

		println "COMPLETE!"
	}

	def static decompile()
	{
		new File(tmp, "decompiled").mkdirs();
		JarBouncer.fernFlower("tmp/extracted", "tmp/decompiled");
	}

	def static deobfuscate()
	{
		def cp = new StringBuilder();
		cp.append("tmp/jars/Minecraft.jar").append(',')
		cp.append("tmp/jars/jinput.jar").append(',')
		cp.append("tmp/jars/lwjgl.jar").append(',')
		cp.append("tmp/jars/lwjgl_util.jar")

		// %(DirJars)s/bin/minecraft.jar,%(DirJars)s/bin/jinput.jar,%(DirJars)s/bin/lwjgl.jar,%(DirJars)s/bin/lwjgl_util.jar
		//JarBouncer.retroGuardDeObf(cp.toString(), resources.path+"/srgs/client_rg.cfg");
		JarBouncer.specialSource(new File(tmp, "jars/Minecraft.jar"), new File(tmp, "Minecraft_ss.jar"), new File(resources, "srgs/client.srg"));
		
	}

	def static downloadStuff()
	{
		def root = new File(tmp, "jars")
		if (!root.exists() || !root.isDirectory())
			root.mkdirs()

		ConfigParser parser = new ConfigParser(resources.path+"/mc_versions.cfg")

		def version = parser.getProperty("default", "current_ver")
		println "downloading Minecraft"
		Util.download(parser.getProperty(version, "client_url"), new File(root, "Minecraft.jar"))

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
