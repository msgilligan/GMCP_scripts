package com.github.abrarsyed.gmcp

import java.util.regex.Matcher

import com.google.common.base.Strings

class FFPatcher
{
	static final MODIFIERS = /public|protected|private|static|abstract|final|native|synchronized|transient|volatile|strict/;
	static final Map<String, String> REG = [
		trailingzero: /(?P<value>[0-9]+\.[0-9]*[1-9])0+(?P<type>[DdFfEe])/,
		empty_super: /(?m)^ +super\(\);\n'/,

		// Remove trailing whitespace
		trailing : /(?m)[ \t]+$/,

		//Remove repeated blank lines
		newlines: /(?m)^\n{2,}/,

		modifiers: /(/ + MODIFIERS + /) /,
		list : /, /,

		enum_class: /(?m)^(?P<modifiers>(?:(?:/ + MODIFIERS + /) )*)(?P<type>enum) (?P<name>[\w$]+)(?: implements (?P<implements>[\w$.]+(?:, [\w$.]+)*))? \{\n(?P<body>(?:.*?\n)*?)(?P<end>\}\n+)/,

		enum_entries: /(?m)^ {3}(?P<name>[\w$]+)\("(?P=name)", [0-9]+(?:, (?P<body>.*?))?\)(?P<end>(?:;|,)\n+)/,

		empty_super: /(?m)^ +super\(\);\n/,

		// strip trailing 0 from doubles and floats to fix decompile differences on OSX
		// 0.0010D => 0.001D
		trailingzero: /(?P<value>[0-9]+\.[0-9]*[1-9])0+(?P<type>[DdFfEe])/,
	];

	static final Map<String, String> REG_FORMAT = [
		constructor : /(?m)^ {3}(?P<modifiers>(?:(?:/ + MODIFIERS + /) )*)%s\((?P<parameters>.*?)\)(?: throws (?P<throws>[\w$.]+(?:, [\w$.]+)*))? \{(?:(?P<empty>\}\n+)|(?:(?P<body>\n(?:.*?\n)*?)(?P<end> {3}\}\n+)))/,

		enumVals: "(?m)^ {3}// \\\$FF: synthetic field\\n {3}private static final %s\\[\\] [\\w\$]+ = new %s\\[\\]\\{.*?\\};\\n",
	];

	//	def _process_file(src_file):
	//	class_name = os.path.splitext(os.path.basename(src_file))[0]
	//	tmp_file = src_file + '.tmp'
	//	with open(src_file, 'r') as fh:
	//		buf = fh.read()
	//
	//	buf = _REGEXP['trailing'].sub(r'', buf)
	//
	//	buf = _REGEXP['enum_class'].sub(enum_match, buf)
	//
	//	buf = _REGEXP['empty_super'].sub(r'', buf)
	//
	//	buf = _REGEXP['trailingzero'].sub(r'\g<value>\g<type>', buf)
	//
	//	buf = _REGEXP['newlines'].sub(r'\n', buf)
	//
	//	with open(tmp_file, 'w') as fh:
	//		fh.write(buf)
	//	shutil.move(tmp_file, src_file)

	def processFile(File file)
	{
		def classname = file.getName().split(/\./)[0];
		def text = file.text;
		text.replaceAll(REG["trailing"], "");

		def enumMatch  =
		{ Matcher match->

			if (classname != match.group('name'))
			{
				println "ERROR PARSING ENUM !!!!! Class Name != File Name";
				return;
			}

			def mods = match.group('modifiers').findAll(REG['modifiers']);
			if (match.group('modifiers') &&  mods.isEmpty())
			{
				println "ERROR PARSING ENUM !!!!! no modifiers!";
				return;
			}

			def interfaces = []
			if (match.group('implements'))
			{
				interfaces = match.group('implements').findAll(REG['list']);
			}

			return processEnum(classname, match.group('type'), mods, interfaces, match.group['body'], match.group['end'])
		};

	}

	def processEnum(classname, classtype, List modifiers, List interfaces, String body, end)
	{
		body.eachMatch(end)
		{ Matcher match ->
			def entryBody = '';
			if (match.group('body'))
			{
				entryBody = "(${match.group('body')})";
			}

			return '   ' + match.group('name') + entryBody + match.group('end');
		};

		def valuesRegex = REG_FORMAT['enumVals'].format(classname, classname);
		body.replaceAll(valuesRegex, "");

		def conRegex = REG_FORMAT['constructor'].format(classname)

		// process constructors
		body.eachMatch(conRegex)
		{ Matcher match ->

			// check modifiers
			def mods = match.group('modifiers').findAll(REG['modifiers']);
			if (match.group('modifiers') &&  mods.isEmpty())
			{
				println "ERROR PARSING ENUM CONSTRUCTOR! !!!!! no modifiers!";
				return;
			}

			def params = [];
			if (match.group('parameters'))
			{
				println "ERROR PARSING ENUM CONSTRUCTOR! !!!!! no modifiers!";
				params = match.group('parameters').split(REG['list']);
				return;
			}

			def exc = [];
			if (match.group('throws'))
			{
				exc = match.group(['throws']).split(REG['list']);
			}

			def methodBody, methodEnd;
			if (!Strings.isNullOrEmpty(match.group('empty')))
			{
				methodBody = '';
				methodEnd = match.group('empty');
			}
			else
			{
				methodBody = match.group('body')
				methodEnd = match.group('end')
			}

			return processConstructor(classname, mods, params, exc, methodBody, methodEnd)
		};

		// rebuild enum
		def out = ''

		if (!modifiers.isEmpty())
		{
			out += modifiers.join(' ')
		}

		out += classtype + ' ' + classname;

		if (!interfaces.isEmpty())
		{
			out += ' implements '+interfaces.join(', ')
		}

		out += "{ \n${body}${end}"

		return out;
	}

	def processConstructor(classname, List<String> mods, List<String> params, List<String> exc, methodBody, methodEnd)
	{
		if (params.size() >= 2)
		{
			// special case?
			if (params.get(0).startsWith('String ') && params.get(1).startsWith('int '))
			{
				params = params.subList(2, params.size()-1);

				// empty constructor
				if (Strings.isNullOrEmpty(body) && params.isEmpty())
				{
					return '';
				}
			}
			else
			{
				println "invalid initial parameters in enum"
				return
			}
			// ERROR
		}
		else
		{
			println "not enough parameters in enum"
			return
		}

		// reuild constructor

		def out = '   ';
		if (mods)
		{
			out += ' '+mods.join(" ")+' '
		}

		out += classname + '(${params.join(", ")})'

		if (exc)
		{
			out += ' throws ${exc.join(", ")}'
		}

		out += ' {${body}${end}'
	}
}