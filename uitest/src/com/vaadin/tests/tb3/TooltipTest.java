/*
 * Copyright 2000-2013 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.tests.tb3;

import java.util.NoSuchElementException;

import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.testbench.By;

/**
 * Base class for TestBench 3+ tests that use tooltips. This class contains
 * utility methods for testing tooltip use.
 * 
 * @author Vaadin Ltd
 */
public abstract class TooltipTest extends MultiBrowserTest {

    protected void clearTooltip() throws Exception {
        moveToRoot();
        Thread.sleep(500);
        checkTooltipNotPresent();
    }

    protected void checkTooltip(String locator, String value) throws Exception {
        checkTooltip(By.vaadin(locator), value);
    }

    protected void checkTooltip(org.openqa.selenium.By by, String value)
            throws Exception {
        checkTooltip(getDriver().findElement(by), value);
    }

    protected void checkTooltip(WebElement element, String value)
            throws Exception {
        testBenchElement(element).showTooltip();
        if (null != value) {
            checkTooltip(value);
        } else {
            checkTooltipNotPresent();
        }
    }

    protected void checkTooltip(String value) throws Exception {
        WebElement tooltip = getTooltip();
        Assert.assertEquals(value, tooltip.getText());
        Assert.assertTrue("Tooltip should be in viewport", tooltip
                .getLocation().getX() >= 0);
    }

    protected void moveToRoot() {
        WebElement uiRoot = getDriver().findElement(By.vaadin("Root"));
        moveMouseToTopLeft(uiRoot);
    }

    protected WebElement getTooltip() throws InterruptedException {
        org.openqa.selenium.By tooltipBy = By.vaadin("Root/VTooltip[0]");
        return getDriver().findElement(tooltipBy);
    }

    protected void checkTooltipNotPresent() throws Exception {
        try {
            WebElement tooltip = getTooltip();
            if (!"".equals(tooltip.getText())
                    || tooltip.getLocation().getX() > -999) {
                Assert.fail("Found tooltip that shouldn't be visible: "
                        + tooltip.getText() + " at " + tooltip.getLocation());
            }
        } catch (NoSuchElementException e) {
            Assert.fail("Tooltip element was removed completely, causing extra events to accessibility tools");
        }
    }

    protected void moveMouseToTopLeft(WebElement element) {
        new Actions(getDriver()).moveToElement(element, 0, 0).perform();
    }

}
