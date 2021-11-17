/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

public abstract class BaseOtaPackageControllerTest extends AbstractControllerTest {

    private IdComparator<OtaPackageInfo> idComparator = new IdComparator<>();

    public static final String TITLE = "My firmware";
    private static final String FILE_NAME = "filename.txt";
    private static final String VERSION = "v1.0";
    private static final String CONTENT_TYPE = "text/plain";
    private static final String CHECKSUM_ALGORITHM = "SHA256";
    private static final String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
    private static final ByteBuffer DATA = ByteBuffer.wrap(new byte[]{1});

    private Tenant savedTenant;
    private User tenantAdmin;
    private DeviceProfileId deviceProfileId;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile", null);
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertNotNull(savedDeviceProfile);
        deviceProfileId = savedDeviceProfile.getId();
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveFirmware() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        Assert.assertNotNull(savedFirmwareInfo);
        Assert.assertNotNull(savedFirmwareInfo.getId());
        Assert.assertTrue(savedFirmwareInfo.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedFirmwareInfo.getTenantId());
        Assert.assertEquals(firmwareInfo.getTitle(), savedFirmwareInfo.getTitle());
        Assert.assertEquals(firmwareInfo.getVersion(), savedFirmwareInfo.getVersion());

        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        save(new SaveOtaPackageInfoRequest(savedFirmwareInfo, false));

        OtaPackageInfo foundFirmwareInfo = doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString(), OtaPackageInfo.class);
        Assert.assertEquals(foundFirmwareInfo.getTitle(), savedFirmwareInfo.getTitle());
    }

    @Test
    public void saveOtaPackageInfoWithViolationOfLengthValidation() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(RandomStringUtils.randomAlphabetic(300));
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);
        doPost("/api/otaPackage", firmwareInfo).andExpect(statusReason(containsString("length of title must be equal or less than 255")));
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(RandomStringUtils.randomAlphabetic(300));
        doPost("/api/otaPackage", firmwareInfo).andExpect(statusReason(containsString("length of version must be equal or less than 255")));
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(true);
        firmwareInfo.setUrl(RandomStringUtils.randomAlphabetic(300));
        doPost("/api/otaPackage", firmwareInfo).andExpect(statusReason(containsString("length of url must be equal or less than 255")));
    }

    @Test
    public void testSaveFirmwareData() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        Assert.assertNotNull(savedFirmwareInfo);
        Assert.assertNotNull(savedFirmwareInfo.getId());
        Assert.assertTrue(savedFirmwareInfo.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedFirmwareInfo.getTenantId());
        Assert.assertEquals(firmwareInfo.getTitle(), savedFirmwareInfo.getTitle());
        Assert.assertEquals(firmwareInfo.getVersion(), savedFirmwareInfo.getVersion());

        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        save(new SaveOtaPackageInfoRequest(savedFirmwareInfo, false));

        OtaPackageInfo foundFirmwareInfo = doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString(), OtaPackageInfo.class);
        Assert.assertEquals(foundFirmwareInfo.getTitle(), savedFirmwareInfo.getTitle());

        MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

        OtaPackageInfo savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);

        Assert.assertEquals(FILE_NAME, savedFirmware.getFileName());
        Assert.assertEquals(CONTENT_TYPE, savedFirmware.getContentType());
        Assert.assertEquals(CHECKSUM_ALGORITHM, savedFirmware.getChecksumAlgorithm().name());
        Assert.assertEquals(CHECKSUM, savedFirmware.getChecksum());
    }

    @Test
    public void testUpdateFirmwareFromDifferentTenant() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        loginDifferentTenant();
        doPost("/api/otaPackage",
                new SaveOtaPackageInfoRequest(savedFirmwareInfo, false),
                OtaPackageInfo.class,
                status().isForbidden());
        deleteDifferentTenant();
    }

    @Test
    public void testFindFirmwareInfoById() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        OtaPackageInfo foundFirmware = doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString(), OtaPackageInfo.class);
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmwareInfo, foundFirmware);
    }

    @Test
    public void testFindFirmwareById() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

        OtaPackageInfo savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);

        OtaPackage foundFirmware = doGet("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString(), OtaPackage.class);
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmware, new OtaPackageInfo(foundFirmware));
        Assert.assertEquals(DATA, foundFirmware.getData());
    }

    @Test
    public void testDeleteFirmware() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        doDelete("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindTenantFirmwares() throws Exception {
        List<OtaPackageInfo> otaPackages = new ArrayList<>();
        for (int i = 0; i < 165; i++) {
            SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
            firmwareInfo.setDeviceProfileId(deviceProfileId);
            firmwareInfo.setType(FIRMWARE);
            firmwareInfo.setTitle(TITLE);
            firmwareInfo.setVersion(VERSION + i);
            firmwareInfo.setUsesUrl(false);

            OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

            if (i > 100) {
                MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

                OtaPackageInfo savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);
                otaPackages.add(savedFirmware);
            } else {
                otaPackages.add(savedFirmwareInfo);
            }
        }

        List<OtaPackageInfo> loadedFirmwares = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<OtaPackageInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/otaPackages?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(otaPackages, idComparator);
        Collections.sort(loadedFirmwares, idComparator);

        Assert.assertEquals(otaPackages, loadedFirmwares);
    }

    @Test
    public void testFindTenantFirmwaresByHasData() throws Exception {
        List<OtaPackageInfo> otaPackagesWithData = new ArrayList<>();
        List<OtaPackageInfo> allOtaPackages = new ArrayList<>();

        for (int i = 0; i < 165; i++) {
            SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
            firmwareInfo.setDeviceProfileId(deviceProfileId);
            firmwareInfo.setType(FIRMWARE);
            firmwareInfo.setTitle(TITLE);
            firmwareInfo.setVersion(VERSION + i);
            firmwareInfo.setUsesUrl(false);

            OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

            if (i > 100) {
                MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

                OtaPackageInfo savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);
                savedFirmwareInfo = new OtaPackageInfo(savedFirmware);
                otaPackagesWithData.add(savedFirmwareInfo);
            }

            allOtaPackages.add(savedFirmwareInfo);
        }

        List<OtaPackageInfo> loadedOtaPackagesWithData = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<OtaPackageInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/otaPackages/" + deviceProfileId.toString() + "/FIRMWARE?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedOtaPackagesWithData.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        List<OtaPackageInfo> allLoadedOtaPackages = new ArrayList<>();
        pageLink = new PageLink(24);
        do {
            pageData = doGetTypedWithPageLink("/api/otaPackages?",
                    new TypeReference<>() {
                    }, pageLink);
            allLoadedOtaPackages.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(otaPackagesWithData, idComparator);
        Collections.sort(allOtaPackages, idComparator);
        Collections.sort(loadedOtaPackagesWithData, idComparator);
        Collections.sort(allLoadedOtaPackages, idComparator);

        Assert.assertEquals(otaPackagesWithData, loadedOtaPackagesWithData);
        Assert.assertEquals(allOtaPackages, allLoadedOtaPackages);
    }

    private OtaPackageInfo save(SaveOtaPackageInfoRequest firmwareInfo) throws Exception {
        return doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
    }

    protected OtaPackageInfo savaData(String urlTemplate, MockMultipartFile content, String... params) throws Exception {
        MockMultipartHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart(urlTemplate, params);
        postRequest.file(content);
        setJwtToken(postRequest);
        return readResponse(mockMvc.perform(postRequest).andExpect(status().isOk()), OtaPackageInfo.class);
    }

}
