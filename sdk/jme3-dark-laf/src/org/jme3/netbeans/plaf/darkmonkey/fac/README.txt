- Note From Charles -
This "Directory" org.jme3.netbeans.plaf.darkmonkay.fac is similarly structured
to the way netbeans handles its Fonts and Colors internally.

It is not really how Netbeans stores it. I have removed "/FontsColors/ThemeName"
from all directories... and the fac can be considered rooted at /Editor

For the actual registration of all these colors... see the layer.xml
file at org.jme3.netbeans.plaf.darkmonkey

That said:
-changing the values in the .xml files *will* affect the color values of the
"Dark Monkey" FAC default profile.

and remember, the Fonts and Colors profile "Dark Monkey" side
of this module is only for the Editor module of Netbeans. Don't confuse this
with the Look and Feel "DarkMonkey" side. :)

Together, they make the theme for DarkMonkey in Netbeans.

Onwards! 
Tips for learners:
-   The syntax for these FAC .xml files is in EditorFontsColor-1_1dtd.txt
    I've included the dtd as a txt so folks have a handy reference for knowing
    what all options they have for including into the tags.
-   Notice that the file names kind of look like the file folder structure of
    one of those exported profile .zip files. Roll with that naming convention
    if you notice the pattern. The last word is the same as the attr assigned
    to the file in the layer.xml file... or the .nbattr file...
-   It's all layered like css, so the more specific you get... the more values
    that have to be overwritten when the user customizes.
-   If in doubt, you can always refer to the Netbeans/Default code to see what
    all "names" need to be covered for completion. (You can look at the 
    <layer in context> from layer.xml and see how the various FAC .xml files 
    work their way through to support the system.)
-   There is a video on the DarkMonkey Dev Blog over at the jMonkeyEngine Forums