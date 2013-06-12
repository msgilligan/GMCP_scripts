package com.github.abrarsyed.gmcp

import argo.jdom.JdomParser
import argo.jdom.JsonRootNode

public class ForgeVersionGetter
{
	private static final JdomParser JDOM_PARSER = new JdomParser()

	def static getUrl(String mc)
	{
		String jsonText = Constants.URL_JSON_FORGE.toURL().text
		JsonRootNode root = JDOM_PARSER.parse(jsonText)

		def builds = root.getArrayNode("builds")
		def files, temp, out

		for (int i = 0; i < builds.size() && out == null; i++)
		{
			files = builds[i].getArrayNode("files")
			
			for (int j = 0; j < files.size() && out == null; j++)
			{
				temp = files[j]
				if (temp.getStringValue("buildtype") == "src" && temp.getStringValue("mcver") == mc)
				{
					// ACTUAL PARSING NOW!
					out = temp.getStringValue("url")
				}
			}
		}
		
		out
	}
}
