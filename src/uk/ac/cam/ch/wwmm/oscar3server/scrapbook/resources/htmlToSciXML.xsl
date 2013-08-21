<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" />
  
<xsl:template match="/">
<P>
<xsl:apply-templates/>
</P>
</xsl:template>

<!-- kill script elements -->
<xsl:template match="script"/>

<xsl:template match="sup[@class='ref']">
	<REF TYPE="P"><xsl:apply-templates/></REF>
</xsl:template>

<xsl:template match="b[@class='xrefc']">
	<XREF TYPE="COMPOUND"><xsl:apply-templates/></XREF>
</xsl:template>

<xsl:template match="sup">
	<SP><xsl:apply-templates/></SP>
</xsl:template>

<xsl:template match="sub">
	<SB><xsl:apply-templates/></SB>
</xsl:template>

<xsl:template match="i">
	<IT><xsl:apply-templates/></IT>
</xsl:template>

<xsl:template match="b">
	<B><xsl:apply-templates/></B>
</xsl:template>

</xsl:stylesheet>