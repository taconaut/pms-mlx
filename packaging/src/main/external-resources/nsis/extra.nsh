; This NSIS header file will be filtered by Maven so some additional
; variables from the pom.xml can be used in NSIS as well. This in
; addition to all the project variables that are already available
; from the "target\project.nsh" that is automatically generated by
; the nsis-maven-plugin.

!define PROJECT_VERSION_SHORT "${project.version.short}"     
!define PROJECT_CORE_BASEDIR "${project.core.basedir}"
!define APPLICATION_NAME "Ps3 Media Server MLX"
!define APPLICATION_NAME_LONG "Ps3 Media Server {project.version}"