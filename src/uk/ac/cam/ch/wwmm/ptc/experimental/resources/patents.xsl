<?xml version="1.0"?>
<xsl:stylesheet 
 version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:param name="ne"/>

	<xsl:template match="/">
		<PAPER>
			<CURRENT_TITLE>
				<xsl:for-each select="/PAPER/HEADER/TITLE">
					<xsl:apply-templates />
				</xsl:for-each>
			</CURRENT_TITLE>
			<BODY>
				<DIV>
					<HEADER>FIELD</HEADER>
					<xsl:for-each select="/PAPER/BODY/FIELD/LINE">
						<P>
							<xsl:apply-templates />
						</P>
					</xsl:for-each>
				</DIV>
				<DIV>
					<HEADER>CLAIMS</HEADER>
					<xsl:for-each select="/PAPER/BODY/CLAIMS">
						<P>
							<xsl:apply-templates />
						</P>
					</xsl:for-each>
				</DIV>
			</BODY>
		</PAPER>
	</xsl:template>

	<xsl:template match="ne">
		<ne type="CHEMICAL"><xsl:apply-templates/></ne>
	</xsl:template>
	<xsl:template match="ligand-name">
		<ne type="LIGAND"><xsl:apply-templates/></ne>
	</xsl:template>
	<xsl:template match="class-name">
		<ne type="CLASS"><xsl:apply-templates/></ne>
	</xsl:template>
	<xsl:template match="formula-name">
		<ne type="FORMULA"><xsl:apply-templates/></ne>
	</xsl:template>

</xsl:stylesheet>