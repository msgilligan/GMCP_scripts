
class Main
{
	public static void main(args)
	{
		println "MAIN IS RUNNING!"
		downloadStuff()
		println "COMPLETE!"
		System.exit(0);
	}

	def static downloadStuff()
	{
		def root = new File("tmp/bin")
		if (!root.exists() || !root.isDirectory())
			root.mkdirs()

		ConfigParser parser = new ConfigParser("mc_versions.cfg")

		def version = parser.getProperty("default", "current_ver")
		Util.download(parser.getProperty(version, "client_url"), root.path+"/"+"minecraft.jar")

		def dls = parser.getProperty("default", "libraries").split(/\s/)
		def url = parser.getProperty("default", "base_url")
		dls.each
		{
			Util.download(url+it, root.path+"/"+it)
		}

		def operating = Util.getOS();

		def natives = parser.getProperty("default", "natives").split(/\s/)[operating.ordinal()]
		Util.download(url+natives, "tmp/"+natives)
		
		def nDir = new File("mc/bin/natives")
		if (!nDir.exists() || !nDir.isDirectory())
			nDir.mkdirs()

		Util.unzip(natives, nDir.path);
		new File(natives).delete();
	}
}
