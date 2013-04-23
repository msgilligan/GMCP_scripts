package com.github.abrarsyed.gmcp

import de.fernflower.main.decompiler.ConsoleDecompiler;

class Main
{
	public static void main(args)
	{	
		//downloadStuff()
		
		println "decompiling !!!!!!!!!!!!"
		decompile()
		
		println "COMPLETE!"
	}
	
	def static decompile()
	{
		Util.unzip("tmp/bin/Minecraft.jar", "tmp/extracted", false)
		
		new File("tmp/decompiled").mkdirs();
		
		def args = new String[7]
		args[0] = "-din=0"
		args[1] = "-rbr=0"
		args[2] = "-dgs=1"
		args[3] = "-asc=1"
		args[4] = "-log=WARN"
		args[5] = "tmp/extracted"
		args[6] = "tmp/decompiled"
		ConsoleDecompiler.main(args);
		// %s -din=0 -rbr=0 -dgs=1 -asc=1 -log=WARN {indir} {outdir}
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
