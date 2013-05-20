package com.github.abrarsyed.gmcp

import java.security.MessageDigest
import java.util.zip.ZipEntry

import com.github.abrarsyed.gmcp.Constants.OperatingSystem

class Util
{

	def static download(String url, File output)
	{
		while(url)
		{
			new URL(url).openConnection().with
			{ conn ->
				conn.instanceFollowRedirects = false
				url = conn.getHeaderField( "Location" )
				if( !url )
				{
					output.withOutputStream
					{ out ->
						conn.inputStream.with
						{ inp ->
							out << inp
							inp.close()
						}
					}
				}
			}
		}
	}

	public static OperatingSystem getOS()
	{
		def name = System.properties["os.name"].toString().toLowerCase()
		if (name.contains("windows"))
			OperatingSystem.WINDOWS
		else if (name.contains("mac"))
			OperatingSystem.MAC
		else if (name.contains("linux"))
			OperatingSystem.LINUX
		else
			null
	}

	def static getSha1(String file)
	{
		int KB = 1024
		int MB = 1024*KB

		File f = new File(file)

		def messageDigest = MessageDigest.getInstance("SHA1")

		long start = System.currentTimeMillis()

		f.eachByte(MB)
		{ byte[] buf, int bytesRead ->
			messageDigest.update(buf, 0, bytesRead)
		}

		def sha1Hex = new BigInteger(1, messageDigest.digest()).toString(16).padLeft( 40, '0' )
		long delta = System.currentTimeMillis()-start
	}

	/**
	 *
	 * @param input      File object of input zip
	 * @param outputDir  File obecjt of output directory
	 * @param stripMeta  Strip the MetaINF or not..
	 */
	def static void unzip(File input, File outputDir, boolean stripMeta)
	{
		def zipFile = new java.util.zip.ZipFile(input)

		outputDir.mkdirs()

		zipFile.entries().each
		{
			def name = ((ZipEntry)it).name

			if (name.endsWith("/") || (stripMeta && name.contains("META-INF")))
			{
				return
			}

			if (name.contains("/"))
			{
				new File(outputDir, name).getParentFile().mkdirs()
			}

			new File(outputDir, name) << zipFile.getInputStream(it).bytes
		}

		zipFile.close()
	}
}
