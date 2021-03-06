require ../../include/gles-control.inc
require include/rcar-gen2-path-common.inc

DESCRIPTION = "SGX/RGX user module"
LICENSE = "CLOSED"

PN = "gles-user-module"
PR = "r0"

RDEPENDS_${PN} = "gles-kernel-module libgbm"

COMPATIBLE_MACHINE = "(r8a7790|r8a7791|r8a7793|r8a7794)"
PACKAGE_ARCH = "${MACHINE_ARCH}"

S_r8a7790 = "${WORKDIR}/rogue"
GLES_r8a7790 = "rgx"
S_r8a7791 = "${WORKDIR}/eurasia"
GLES_r8a7791 = "sgx"
S_r8a7793 = "${WORKDIR}/eurasia"
GLES_r8a7793 = "sgx"
S_r8a7794 = "${WORKDIR}/eurasia"
GLES_r8a7794 = "sgx"

FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"

OPENGLES3 ?= "0"
SRC_URI_r8a7790 = '${@base_conditional( "OPENGLES3", "1", \
        "file://r8a7790_linux_rgx_binaries_gles3.tar.bz2", \
        "file://r8a7790_linux_rgx_binaries_gles2.tar.bz2", d )}'
SRC_URI_append_r8a7790 = " ${@bb.utils.contains("DISTRO_FEATURES", "wayland", " \
        file://EGL_headers_for_wayland.patch \
        file://change-shell.patch \
        ", "", d)}"

SRC_URI_r8a7791 = "file://r8a7791_linux_sgx_binaries_gles2.tar.bz2"
SRC_URI_append_r8a7791 = " ${@bb.utils.contains("DISTRO_FEATURES", "wayland", " \
        file://EGL_headers_for_wayland.patch \
        ", "", d)}"

SRC_URI_r8a7793 = "file://r8a7791_linux_sgx_binaries_gles2.tar.bz2"
SRC_URI_append_r8a7793 = " ${@bb.utils.contains("DISTRO_FEATURES", "wayland", " \
        file://EGL_headers_for_wayland.patch \
        ", "", d)}"

SRC_URI_r8a7794 = "file://r8a7794_linux_sgx_binaries_gles2.tar.bz2"
SRC_URI_append_r8a7794 = " ${@bb.utils.contains("DISTRO_FEATURES", "wayland", " \
        file://EGL_headers_for_wayland.patch \
        ", "", d)}"

SRC_URI_append = " file://rc.pvr.service "

inherit systemd

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE_${PN} = "rc.pvr.service"

do_populate_lic[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    # Copy binary into sysroot
    cp -r ${S}/etc ${D}
    cp -r ${S}/usr ${D}
    mkdir -p ${D}${RENESAS_DATADIR}
    mv ${D}/usr/local/* ${D}${RENESAS_DATADIR}
    rm -fr ${D}/usr/local
    mv ${D}/etc/init.d/rc.pvr ${D}${RENESAS_DATADIR}/bin/
    sed -i "s,/usr/local,${RENESAS_DATADIR},g" \
           ${D}${RENESAS_DATADIR}/bin/rc.pvr
    
    # Create a symbolic link for compatibility with various software
    ln -s libGLESv2.so ${D}/usr/lib/libGLESv2.so.2

    if [ "${USE_WAYLAND}" = "1" ]; then
        # Rename libEGL.so
        mv ${D}/usr/lib/libEGL.so ${D}/usr/lib/libEGL-pvr.so
         

        # Set the "WindowSystem" parameter for wayland
        if [ "${GLES}" = "rgx" ]; then
           sed -i -e "s/WindowSystem=libpvrNULL_WSEGL.so/WindowSystem=libpvrWAYLAND_WSEGL.so/g" \
           ${D}/${sysconfdir}/powervr.ini
        elif [ "${GLES}" = "sgx" ]; then
           sed -i -e "s/WindowSystem=libpvrPVR2D_FLIPWSEGL.so/WindowSystem=libpvrPVR2D_WAYLANDWSEGL.so/g" \
           ${D}/${sysconfdir}/powervr.ini
        fi
    fi
    # Install systemd unit files
    if ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'true', 'false', d)}; then
        install -m 644 -p -D ${WORKDIR}/rc.pvr.service ${D}${systemd_system_unitdir}/rc.pvr.service
        sed -i "s,@RENESAS_DATADIR@,${RENESAS_DATADIR},g" \
               ${D}${systemd_system_unitdir}/rc.pvr.service
    fi
} 

PACKAGES = "\
    ${PN} \
    ${PN}-dev \
"

FILES_${PN} = " \
    ${sysconfdir}/* \
    ${libdir}/* \
    ${RENESAS_DATADIR}/bin/* \
"

FILES_${PN}-dev = " \
    ${includedir}/* \
"

PROVIDES = "virtual/libgles2"
PROVIDES_append = "${@bb.utils.contains("DISTRO_FEATURES", "wayland", "", " virtual/egl", d)}"
RPROVIDES_${PN} += "${GLES}-user-module libgles2-mesa libgles2-mesa-dev libgles2 libgles2-dev"
INSANE_SKIP_${PN} += "ldflags already-stripped"
INSANE_SKIP_${PN}-dev += "ldflags"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
PRIVATE_LIBS_${PN} = "libEGL.so.1"
