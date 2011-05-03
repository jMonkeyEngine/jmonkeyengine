<?xml version="1.0"?> 

<!--
    Copyright (c) 2001-2009, Jean Tessier
    All rights reserved.
    
    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:
    
        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
    
        * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in the
          documentation and/or other materials provided with the distribution.
    
        * Neither the name of Jean Tessier nor the names of his contributors
          may be used to endorse or promote products derived from this software
          without specific prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
    "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
    LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
    A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR
    CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:strip-space elements="*"/>

    <xsl:template match="differences">
        <html>

        <head>
            <title><xsl:if test="name/text()"><xsl:value-of select="name"/> - </xsl:if>API Change History</title>
        </head>

        <body bgcolor="#ffffff">

        <h1><xsl:if test="name/text()"><xsl:value-of select="name"/> - </xsl:if>API Change History</h1>

        <ul>
        <li><a href="#{new}"><xsl:value-of select="old"/> to <xsl:value-of select="new"/></a></li>
        </ul>

        <hr />

        <a name="{new}" />
        <h2><xsl:value-of select="old"/> to <xsl:value-of select="new"/></h2>

        <xsl:apply-templates/>

        <hr />

        </body>

        </html>
    </xsl:template>

    <xsl:template match="differences/name | old | new"/>

    <xsl:template match="removed-packages">
        <h3>Removed Packages:</h3>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>

    <xsl:template match="removed-interfaces">
        <h3>Removed Interfaces:</h3>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="removed-classes">
        <h3>Removed Classes:</h3>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="deprecated-interfaces">
        <h3>Newly Deprecated Interfaces:</h3>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="deprecated-classes">
        <h3>Newly Deprecated Classes:</h3>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>

    <xsl:template match="modified-interfaces">
        <h3>Modified Interfaces:</h3>
        <blockquote>
        <xsl:apply-templates/>
        </blockquote>
    </xsl:template>
 
    <xsl:template match="modified-classes">
        <h3>Modified Classes:</h3>
        <blockquote>
        <xsl:apply-templates/>
        </blockquote>
    </xsl:template>

    <xsl:template match="undeprecated-interfaces">
        <h3>Formerly Deprecated Interfaces:</h3>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="undeprecated-classes">
        <h3>Formerly Deprecated Classes:</h3>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="new-packages">
        <h3>New Packages:</h3>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>

    <xsl:template match="new-interfaces">
        <h3>New Interfaces:</h3>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="new-classes">
        <h3>New Classes:</h3>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="class">
        <h4><code><xsl:value-of select="name"/></code></h4>
        <blockquote>
            <xsl:apply-templates/>
        </blockquote>
    </xsl:template>

    <xsl:template match="removed-fields">
        <h5>Removed Fields:</h5>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="removed-constructors">
        <h5>Removed Constructors:</h5>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="removed-methods">
        <h5>Removed Methods:</h5>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="deprecated-fields">
        <h5>Newly Deprecated Fields:</h5>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="deprecated-constructors">
        <h5>Newly Deprecated Constructors:</h5>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="deprecated-methods">
        <h5>Newly Deprecated Methods:</h5>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>

    <xsl:template match="modified-fields">
        <h5>Field Declaration Changes:</h5>
        <xsl:apply-templates/>
    </xsl:template>
 
    <xsl:template match="modified-constructors">
        <h5>Constructor Changes:</h5>
        <xsl:apply-templates/>
    </xsl:template>
 
    <xsl:template match="modified-methods">
        <h5>Method Changes:</h5>
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="feature">
        <blockquote>
        <p><nobr><code>
            <xsl:apply-templates/>
        </code></nobr></p>
        </blockquote>
    </xsl:template>

    <xsl:template match="modified-code">
        <b>code:</b> <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="modified-declaration">
        <b>old:</b> <xsl:value-of select="old-declaration"/>
        <xsl:if test="old-declaration[@deprecated='yes']"> <b>[deprecated]</b></xsl:if>
        <br/>
        <b>new:</b> <xsl:value-of select="new-declaration"/>
        <xsl:if test="new-declaration[@deprecated='yes']"> <b>[deprecated]</b></xsl:if>
	<xsl:if test="../modified-code"><br/></xsl:if>
    </xsl:template>
 
    <xsl:template match="undeprecated-fields">
        <h5>Formerly Deprecated Fields:</h5>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="undeprecated-constructors">
        <h5>Formerly Deprecated Constructors:</h5>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="undeprecated-methods">
        <h5>Formerly Deprecated Methods:</h5>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="new-fields">
        <h5>New Fields:</h5>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="new-constructors">
        <h5>New Constructors:</h5>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
 
    <xsl:template match="new-methods">
        <h5>New Methods:</h5>
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>

    <xsl:template match="class/name | feature/name"/>

    <xsl:template match="class/modified-declaration">
        <h5>Declaration Changes:</h5>
        <blockquote>
        <p><nobr><code>
        <b>old:</b> <xsl:value-of select="old-declaration"/>
        <xsl:if test="old-declaration[@deprecated='yes']"> <b>[deprecated]</b></xsl:if>
        <br/>
        <b>new:</b> <xsl:value-of select="new-declaration"/>
        <xsl:if test="new-declaration[@deprecated='yes']"> <b>[deprecated]</b></xsl:if>
        </code></nobr></p>
        </blockquote>
    </xsl:template>

    <xsl:template match="new-packages/name[@deprecated='yes'] | new-interfaces/name[@deprecated='yes'] | new-classes/name[@deprecated='yes'] | new-fields/declaration[@deprecated='yes'] | new-constructors/declaration[@deprecated='yes'] | new-methods/declaration[@deprecated='yes']">
        <li><nobr><code><xsl:value-of select="."/> <b>[deprecated]</b></code></nobr></li>
    </xsl:template>

    <xsl:template match="name | declaration">
        <li><nobr><code><xsl:value-of select="."/></code></nobr></li>
    </xsl:template>

</xsl:stylesheet>
