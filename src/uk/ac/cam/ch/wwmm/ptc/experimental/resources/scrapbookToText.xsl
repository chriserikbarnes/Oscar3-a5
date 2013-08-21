<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text"/>

	<xsl:template match="/">
		<xsl:for-each select="//snippet">
			<xsl:text>
			
			</xsl:text><xsl:apply-templates /><xsl:text>
			
			</xsl:text>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="REF"></xsl:template>

	<xsl:template match="ne[@type='CM']">
		<xsl:text>OSCARCOMPOUND</xsl:text>
	</xsl:template>

	<xsl:template match="ne[@type='RN']">
		<xsl:text>OSCARREACTION</xsl:text>
	</xsl:template>

	<xsl:template match="ne[@type='CJ']">
		<xsl:text>OSCARADJECTIVE</xsl:text>
	</xsl:template>

	<xsl:template match="ne[@type='ASE']">
		<xsl:text>OSCARENZYME</xsl:text>
	</xsl:template>

	<xsl:template match="ne[@type='CPR']">
		<xsl:text>OSCARPREFIX</xsl:text>
	</xsl:template>


</xsl:stylesheet>