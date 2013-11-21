/*
 * [The "BSD licence"]
 * Copyright (c) 2012 Dandelion
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of Dandelion nor the names of its contributors 
 * may be used to endorse or promote products derived from this software 
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.dandelion.datatables.core.configuration;

import static org.fest.assertions.Assertions.assertThat;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;

import com.github.dandelion.datatables.core.exception.BadConfigurationException;
import com.github.dandelion.datatables.core.export.ExportLinkPosition;
import com.github.dandelion.datatables.core.export.ExportType;
import com.github.dandelion.datatables.core.extension.feature.PaginationType;
import com.github.dandelion.datatables.core.extension.theme.Bootstrap2Theme;
import com.github.dandelion.datatables.core.extension.theme.Theme;

public class TableConfigurationTest {

	private HttpServletRequest request;

	@Before
	public void setup() throws BadConfigurationException {
		MockServletContext mockServletContext = new MockServletContext();
		MockPageContext mockPageContext = new MockPageContext(mockServletContext);
		request = (HttpServletRequest) mockPageContext.getRequest();
	}
	
	@Test
	public void should_init_configuration_from_properties() {
		TableConfiguration tableConfiguration = TableConfiguration.getInstance(request);

		assertThat(tableConfiguration.getFeatureInfo()).isNull();
		assertThat(tableConfiguration.getMainAggregatorEnable()).isEqualTo(false);
		assertThat(tableConfiguration.getAjaxPipeSize()).isEqualTo(5);

		// Export configurations
		assertThat(tableConfiguration.getExportConfs()).isNull();
		assertThat(tableConfiguration.getExportLinkPositions()).isNull();
		assertThat(tableConfiguration.getExportClass(ExportType.XLS)).isEqualTo(
				"com.github.dandelion.datatables.extras.export.poi.XlsExport");
		assertThat(tableConfiguration.getExportClass(ExportType.CSV)).isEqualTo(
				"com.github.dandelion.datatables.core.export.CsvExport");
		assertThat(tableConfiguration.getPluginFixedHeader()).isEqualTo(false);
	}

	@Test
	public void should_override_global_configuration_with_specific_programmatically() {
		TableConfiguration tableConfiguration = TableConfiguration.getInstance(request);

		tableConfiguration.setMainCompressorEnable(true).setFeaturePaginationType(PaginationType.INPUT)
				.setCssTheme(new Bootstrap2Theme()).setAjaxPipeSize(12);

		assertThat(tableConfiguration.getFeatureInfo()).isNull();
		assertThat(tableConfiguration.getMainCompressorEnable()).isEqualTo(true);
		assertThat(tableConfiguration.getFeaturePaginationType()).isEqualTo(PaginationType.INPUT);
		assertThat(tableConfiguration.getCssTheme()).isEqualTo(Theme.BOOTSTRAP2.getInstance());
		assertThat(tableConfiguration.getAjaxPipeSize()).isEqualTo(12);
	}
}