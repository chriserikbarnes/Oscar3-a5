<?xml version="1.0" encoding="UTF-8" ?>

<!-- xsl to  -->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
        <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
       </xsl:template>      
	<!-- This removes all the ART annotations -->
	<xsl:template match="annotationART">
		<xsl:apply-templates select="node()"/>
     </xsl:template> 
</xsl:stylesheet>
