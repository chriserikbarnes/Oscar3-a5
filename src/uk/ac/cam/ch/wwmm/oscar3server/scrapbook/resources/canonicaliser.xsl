<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" />
  
<xsl:template match="/">
<snippet>
<xsl:apply-templates/>
</snippet>
</xsl:template>

<!-- kill script elements -->
<xsl:template match="script"/>

<xsl:template match="REF">
	<REF TYPE="P"><xsl:apply-templates/></REF>
</xsl:template>

<xsl:template match="XREF">
	<XREF TYPE="COMPOUND"><xsl:apply-templates/></XREF>
</xsl:template>

<xsl:template match="SP">
	<SP><xsl:apply-templates/></SP>
</xsl:template>

<xsl:template match="SB">
	<SB><xsl:apply-templates/></SB>
</xsl:template>

<xsl:template match="IT">
	<IT><xsl:apply-templates/></IT>
</xsl:template>

<xsl:template match="B">
	<B><xsl:apply-templates/></B>
</xsl:template>

<xsl:template match="ne">
	<xsl:element name="ne">
		<xsl:attribute name="type"><xsl:value-of select="@type"/></xsl:attribute>
		<xsl:apply-templates/>
	</xsl:element>
</xsl:template>

</xsl:stylesheet>