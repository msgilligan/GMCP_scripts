
class Main
{
	public static void main(args)
	{
		downloadStuff()
		println "COMPLETE!"
	}
	
	def static decompile()
	{
		//
	}
	
	def static downloadStuff()
	{
		def root = new File("tmp/bin")
		if (!root.exists() || !root.isDirectory())
			root.mkdirs()

		ConfigParser parser = new ConfigParser("mc_versions.cfg")

		def version = parser.getProperty("default", "current_ver")
		println "downlaoding Minecraft"
		Util.download(parser.getProperty(version, "client_url"), root.path+"/"+"minecraft.jar")

		def dls = parser.getProperty("default", "libraries").split(/\s/)
		def url = parser.getProperty("default", "base_url")
		println "downlaoding libraries"
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

		Util.unzip("tmp/"+natives, nDir.path)
		new File("tmp/"+natives).delete()
	}
}
