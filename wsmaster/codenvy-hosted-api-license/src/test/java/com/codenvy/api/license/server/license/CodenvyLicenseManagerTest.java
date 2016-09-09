/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.license.server.license;

import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.NameGenerator;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 * @author Alexander Andrienko
 */
@Listeners(value = {MockitoTestNGListener.class})
public class CodenvyLicenseManagerTest {

    private static final String TEXT                       = "some test";
    private static final String LICENSE_STORAGE_PREFIX_DIR = "licenseStorage-";
    private static final String LICENSE                    = "license";

    @Mock
    private CodenvyLicenseFactory licenseFactory;
    @Mock
    private CodenvyLicense        codenvyLicense;

    private File testDirectory;
    private File licenseFile;

    private CodenvyLicenseManager codenvyLicenseManager;

    @BeforeMethod
    public void setUp() throws IOException {
        File targetDir = new File(Thread.currentThread().getContextClassLoader().getResource(".").getPath()).getParentFile();
        testDirectory = new File(targetDir, NameGenerator.generate(LICENSE_STORAGE_PREFIX_DIR, 4));
        licenseFile = new File(testDirectory, LICENSE);
        Files.createDirectories(testDirectory.toPath());
        codenvyLicenseManager = new CodenvyLicenseManager(licenseFile.getAbsolutePath(), licenseFactory);

        Mockito.when(codenvyLicense.getLicenseText()).thenReturn(TEXT);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        IoUtil.deleteRecursive(testDirectory);
    }

    @Test
    public void licenseShouldBeStored() throws Exception {
        codenvyLicenseManager.store(codenvyLicense);

        verify(codenvyLicense).getLicenseText();
        assertTrue(Files.exists(licenseFile.toPath()));
        String fileContent = new String(Files.readAllBytes(licenseFile.toPath()), UTF_8);
        assertEquals(TEXT, fileContent);
    }

    @Test
    public void licenseShouldBeDeleted() throws Exception {
        codenvyLicenseManager.store(codenvyLicense);

        codenvyLicenseManager.delete();
        assertFalse(Files.exists(licenseFile.toPath()));
    }

    @Test(expectedExceptions = LicenseNotFoundException.class)
    public void shouldThrowLicenseNotFoundExceptionIfWeTryToDeleteLicenseFromEmptyStorage() {
        codenvyLicenseManager.delete();
    }

    @Test
    public void licenseShouldBeLoaded() {
        when(licenseFactory.create(TEXT)).thenReturn(codenvyLicense);
        codenvyLicenseManager.store(codenvyLicense);

        CodenvyLicense license = codenvyLicenseManager.load();

        verify(licenseFactory).create(TEXT);
        assertEquals(license, codenvyLicense);
    }

    @Test(expectedExceptions = LicenseNotFoundException.class)
    public void shouldThrowLicenseNotFoundExceptionIfWeTryToGetLicenseFromEmptyStorage() {
        codenvyLicenseManager.load();
    }
}
