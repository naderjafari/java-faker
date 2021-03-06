/*
 * The MIT License
 * Copyright © 2018 Edwin Njeru
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.javafaker.service;

import com.github.javafaker.AbstractFakerTest;
import com.github.javafaker.Faker;
import com.github.javafaker.component.Superhero;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.github.javafaker.matchers.MatchesRegularExpression.matchesRegularExpression;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class FakeValuesServiceTest extends AbstractFakerTest {

    @Mock
    private RandomService randomService;

    private FakeValuesService fakeValuesService;

    @Before
    public void before() {
        super.before();
        MockitoAnnotations.initMocks(this);

        // always return the first element
        when(randomService.nextInt(anyInt())).thenReturn(0);

        fakeValuesService = spy(new FakeValuesService(new Locale("test"), randomService));
    }

    @Test(expected = LocaleDoesNotExistException.class)
    public void localeShouldThrowException() {
        new FakeValuesService(new Locale("Does not exist"), randomService);
    }

    @Test
    public void fetchStringShouldReturnValue() {
        assertThat(fakeValuesService.fetchString("property.dummy"), is("x"));
    }

    @Test
    public void fetchShouldReturnValue() {
        assertThat(fakeValuesService.fetch("property.dummy"), Is.<Object>is("x"));
    }

    @Test
    public void fetchObjectShouldReturnValue() {
        assertThat(fakeValuesService.fetchObject("property.dummy"), Is.<Object>is(Arrays.asList("x", "y", "z")));
    }

    @Test
    public void safeFetchShouldReturnValueInList() {
        doReturn(0).when(randomService).nextInt(Mockito.anyInt());
        assertThat(fakeValuesService.safeFetch("property.dummy", null), is("x"));
    }

    @Test
    public void safeFetchShouldReturnSimpleList() {
        assertThat(fakeValuesService.safeFetch("property.simple", null), is("hello"));
    }

    @Test
    public void safeFetchShouldReturnEmptyStringWhenPropertyDoesntExist() {
        assertThat(fakeValuesService.safeFetch("property.dummy2", ""), isEmptyString());
    }

    @Test
    public void bothify2Args() {
        final DummyService dummy = mock(DummyService.class);

        Faker f = new Faker();

        String value = fakeValuesService.resolve("property.bothify_2", dummy, f);
        assertThat(value, matchesRegularExpression("[A-Z]{2}\\d{2}"));
    }

    @Test
    public void regexifyDirective() {
        final DummyService dummy = mock(DummyService.class);

        String value = fakeValuesService.resolve("property.regexify1", dummy, faker);
        assertThat(value, isOneOf("55", "44", "45", "54"));
        verify(faker).regexify("[45]{2}");
    }

    @Test
    public void regexifySlashFormatDirective() {
        final DummyService dummy = mock(DummyService.class);

        String value = fakeValuesService.resolve("property.regexify_slash_format", dummy, faker);
        assertThat(value, isOneOf("55", "44", "45", "54"));
        verify(faker).regexify("[45]{2}");
    }

    @Test
    public void regexifyDirective2() {
        final DummyService dummy = mock(DummyService.class);

        String value = fakeValuesService.resolve("property.regexify_cell", dummy, faker);
        assertThat(value, isOneOf("479", "459"));
        verify(faker).regexify("4[57]9");
    }

    @Test
    public void resolveKeyToPropertyWithAPropertyWithoutAnObject() {
        // #{hello} -> DummyService.hello

        // given
        final DummyService dummy = mock(DummyService.class);
        doReturn("Yo!").when(dummy).hello();

        // when
        final String actual = fakeValuesService.resolve("property.simpleResolution", dummy, faker);

        // then
        assertThat(actual, is("Yo!"));
        verify(dummy).hello();
        verifyZeroInteractions(faker);
    }

    @Test
    public void resolveKeyToPropertyWithAPropertyWithAnObject() {
        // given
        final Superhero person = mock(Superhero.class);
        final DummyService dummy = mock(DummyService.class);
        doReturn(person).when(faker).superhero();
        doReturn("Luke Cage").when(person).name();

        // when
        final String actual = fakeValuesService.resolve("property.advancedResolution", dummy, faker);

        // then
        assertThat(actual, is("Luke Cage"));
        verify(faker).superhero();
        verify(person).name();
    }

    @Test
    public void resolveKeyToPropertyWithAList() {
        // property.resolutionWithList -> #{hello}
        // #{hello} -> DummyService.hello

        // given
        final DummyService dummy = mock(DummyService.class);
        doReturn(0).when(randomService).nextInt(Mockito.anyInt());
        doReturn("Yo!").when(dummy).hello();

        // when
        final String actual = fakeValuesService.resolve("property.resolutionWithList", dummy, faker);

        // then
        assertThat(actual, is("Yo!"));
        verify(dummy).hello();
    }

    @Test
    public void resolveKeyWithMultiplePropertiesShouldJoinResults() {
        // given
        final Superhero person = mock(Superhero.class);
        final DummyService dummy = mock(DummyService.class);
        doReturn(person).when(faker).superhero();

        doReturn("Yo Superman!").when(dummy).hello();
        doReturn("up up and away").when(person).descriptor();

        // when
        String actual = fakeValuesService.resolve("property.multipleResolution", dummy, faker);

        // then
        assertThat(actual, is("Yo Superman! up up and away"));

        verify(faker).superhero();
        verify(person).descriptor();
        verify(dummy).hello();
    }

    @Test
    public void testLocaleChain() {
        final List<Locale> chain = fakeValuesService.localeChain(Locale.SIMPLIFIED_CHINESE);

        assertThat(chain, contains(Locale.SIMPLIFIED_CHINESE, Locale.CHINESE, Locale.ENGLISH));
    }

    @Test
    public void testLocaleChainEnglish() {
        final List<Locale> chain = fakeValuesService.localeChain(Locale.ENGLISH);

        assertThat(chain, contains(Locale.ENGLISH));
    }

    @Test
    public void testLocaleChainLanguageOnly() {
        final List<Locale> chain = fakeValuesService.localeChain(Locale.CHINESE);

        assertThat(chain, contains(Locale.CHINESE, Locale.ENGLISH));
    }

    @Test
    public void expressionWithInvalidFakerObject() {
        expressionShouldFailWith("#{ObjectNotOnFaker.methodName}", "Unable to resolve #{ObjectNotOnFaker.methodName} directive.");
    }

    @Test
    public void expressionWithValidFakerObjectButInvalidMethod() {
        expressionShouldFailWith("#{Name.nonExistentMethod}", "Unable to resolve #{Name.nonExistentMethod} directive.");
    }

    /**
     * Two things are important here:
     * 1) the message in the exception should be USEFUL
     * 2) a {@link RuntimeException} should be thrown.
     * <p>
     * if the message changes, it's ok to update the test provided
     * the two conditions above are still true.
     */
    @Test
    public void expressionWithValidFakerObjectValidMethodInvalidArgs() {
        expressionShouldFailWith("#{Number.number_between 'x','y'}", "Unable to resolve #{Number.number_between 'x','y'} directive.");
    }

    /**
     * Two things are important here:
     * 1) the message in the exception should be USEFUL
     * 2) a {@link RuntimeException} should be thrown.
     * <p>
     * if the message changes, it's ok to update the test provided
     * the two conditions above are still true.
     */
    @Test
    public void expressionCompletelyUnresolvable() {
        expressionShouldFailWith("#{x}", "Unable to resolve #{x} directive.");
    }

    private void expressionShouldFailWith(String expression, String errorMessage) {
        try {
            fakeValuesService.expression(expression, faker);
            fail("Should have failed with RuntimeException and message of " + errorMessage);
        } catch (RuntimeException re) {
            assertThat(re.getMessage(), is(errorMessage));
        }
    }

    public static class DummyService {
        public String firstName() {
            return "John";
        }

        public String lastName() {
            return "Smith";
        }

        public String hello() {
            return "Hello";
        }
    }
}
