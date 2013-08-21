<xsl:stylesheet 
 version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:z="http://foo.bar">

<xsl:template match="wrap">
	<PAPER>
		<xsl:apply-templates/>
	</PAPER>
</xsl:template>

<xsl:template match="text">
	<BODY>
		<xsl:apply-templates/>
	</BODY>
</xsl:template>

<xsl:template match="SENT">
	<P>
		<xsl:apply-templates/>
	</P>
</xsl:template>

<xsl:template match="plain">
	<DUMMY>
		<xsl:apply-templates/>
	</DUMMY>
</xsl:template>

<xsl:template match="z:chebi">
	<DUMMY>
		<xsl:apply-templates/>
	</DUMMY>
</xsl:template>

</xsl:stylesheet>