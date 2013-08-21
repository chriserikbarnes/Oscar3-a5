<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:cml="http://www.xml-cml.org/schema">
  <xsl:output method="html" indent="yes"/>

	<xsl:param name="jarpath" select="/processing-instruction('jarpath')"/>
	<xsl:param name="host" select="/processing-instruction('host')"/>
	<xsl:param name="viewer" select="/processing-instruction('viewer')"/>
	<xsl:param name="printable" select="/processing-instruction('printable')"/>
  
  <xsl:template match="/">
    <html>
      <head>
		<title>Foo</title>
	  </head>
	  <body>
	  	<table border="1">
		  <xsl:for-each select="//dictEntry">
		  	<tr>
		  	  <td><xsl:value-of select="@name"/></td>
		  	  <td rowspan="4"><img><xsl:attribute name="src"><xsl:value-of select="@pngfile"/></xsl:attribute></img></td>
		  	</tr>
		  	<tr>
		  		<td>Filename: <tt><xsl:value-of select="@filename"/></tt></td>
		  	</tr>
		  	<tr>
		  		<td>SMILES: <tt><xsl:value-of select="@smiles"/></tt></td>
		  	</tr>
		  	<tr>
		  		<td>InChI: <tt><xsl:value-of select="@inchi"/></tt></td>		  	
		  	</tr>
		  </xsl:for-each>
		</table>
	  </body>
	</html>
  </xsl:template>
 </xsl:stylesheet>
