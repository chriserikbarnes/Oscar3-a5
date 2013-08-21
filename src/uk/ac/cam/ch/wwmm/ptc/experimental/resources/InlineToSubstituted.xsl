<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		<doc><xsl:apply-templates /></doc>
	</xsl:template>

	<xsl:template match="ne">
		<xsl:text>NAMEDENTITY</xsl:text>
	</xsl:template>

</xsl:stylesheet>