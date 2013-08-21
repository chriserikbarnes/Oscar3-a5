<?xml version="1.0"?>
<xsl:stylesheet 
 version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:param name="ne"/>

<xsl:template match="/">
<PAPER>
<TITLE>
	<xsl:for-each select="//title">
		<xsl:call-template name="block"/>
	</xsl:for-each>
</TITLE>
<ABSTRACT>
	<xsl:for-each select="//abstract">
		<xsl:call-template name="block"/>
	</xsl:for-each>
</ABSTRACT>
</PAPER>
</xsl:template>

<xsl:template name="block">
	<xsl:for-each select="sentence">
		<xsl:apply-templates/>
		<xsl:if test="following-sibling::sentence">
		<xsl:text> </xsl:text>
		</xsl:if>
	</xsl:for-each>
</xsl:template>

<xsl:template match="title">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="abstract">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="cons[@sem='G#other_organic_compound'] |
					cons[@sem='G#amino_acid_monomer'] |
					cons[@sem='G#atom'] |
					cons[@sem='G#inorganic'] |
					cons[@sem='G#lipid'] |
					cons[@sem='G#nucleotide'] |
					cons[@sem='G#carbohydrate']">
	<xsl:choose>
		<xsl:when test="$ne">
			<ne type="CM">
				<xsl:apply-templates mode="notemplates"/>
			</ne>		
		</xsl:when>
		<xsl:otherwise>
			<xsl:apply-templates mode="notemplates"/>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

</xsl:stylesheet>