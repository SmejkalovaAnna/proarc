/*
 * Copyright (C) 2012 Jan Pokorsky
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.pas.editor.client.widget;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IconButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;
import com.smartgwt.client.widgets.form.validator.RequiredIfFunction;
import com.smartgwt.client.widgets.form.validator.RequiredIfValidator;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import cz.incad.pas.editor.client.PasEditorMessages;
import cz.incad.pas.editor.client.action.Actions;
import cz.incad.pas.editor.client.action.DigitalObjectSelector;
import cz.incad.pas.editor.client.action.RefreshAction;
import cz.incad.pas.editor.client.ds.MetaModelDataSource;
import cz.incad.pas.editor.client.ds.RestConfig;
import cz.incad.pas.editor.client.ds.SearchDataSource;
import cz.incad.pas.editor.shared.rest.DigitalObjectResourceApi;
import cz.incad.pas.editor.shared.rest.DigitalObjectResourceApi.SearchType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 * Shows list of digital objects.
 *
 * @author Jan Pokorsky
 */
public final class DigitalObjectSearchView implements DigitalObjectSelector, RefreshAction.Refreshable {
    
    private static final Logger LOG = Logger.getLogger(DigitalObjectSearchView.class.getName());
    private static final String FILTER_LAST_CREATED = SearchType.LAST_CREATED.toString();
    private static final String FILTER_LAST_MODIFIED = SearchType.LAST_MODIFIED.toString();
    private static final String FILTER_PHRASE = SearchType.PHRASE.toString();
    private static final String FILTER_QUERY = SearchType.QUERY.toString();

    private final DynamicForm filters;
    private final Canvas rootWidget;
    private final ListGrid foundGrid;
    private final PasEditorMessages i18nPas;

    public DigitalObjectSearchView(PasEditorMessages i18nPas) {
        this.i18nPas = i18nPas;

        foundGrid = createList();
        
        filters = createFilter();
        filters.setVisible(false);

        VLayout vLayout = new VLayout();
        vLayout.addMember(filters);
        vLayout.addMember(createToolbar());
        vLayout.addMember(foundGrid);
        rootWidget = vLayout;
    }

    private ListGrid createList() {
        ListGrid grid = new ListGrid();
        grid.setSelectionType(SelectionStyle.SINGLE);
        grid.setCanSort(false);
        grid.setDataSource(SearchDataSource.getInstance());

        ListGridField label = new ListGridField(SearchDataSource.FIELD_LABEL,
                i18nPas.DigitalObjectSearchView_ListHeaderLabel_Title());
        ListGridField model = new ListGridField(SearchDataSource.FIELD_MODEL,
                i18nPas.DigitalObjectSearchView_ListHeaderModel_Title());
        model.setOptionDataSource(MetaModelDataSource.getInstance());
        model.setValueField(MetaModelDataSource.FIELD_PID);
        model.setDisplayField(MetaModelDataSource.FIELD_DISPLAY_NAME);
        ListGridField pid = new ListGridField(SearchDataSource.FIELD_PID,
                i18nPas.DigitalObjectSearchView_ListHeaderPid_Title());
        ListGridField created = new ListGridField(SearchDataSource.FIELD_CREATED,
                i18nPas.DigitalObjectSearchView_ListHeaderCreated_Title());
        ListGridField modified = new ListGridField(SearchDataSource.FIELD_MODIFIED,
                i18nPas.DigitalObjectSearchView_ListHeaderModified_Title());
        ListGridField owner = new ListGridField(SearchDataSource.FIELD_OWNER,
                i18nPas.DigitalObjectSearchView_ListHeaderOwner_Title());
        ListGridField state = new ListGridField(SearchDataSource.FIELD_STATE,
                i18nPas.DigitalObjectSearchView_ListHeaderState_Title());
        grid.setFields(label, model, pid, created, modified, owner, state);
        return grid;
    }

    private Canvas createToolbar() {
        ToolStrip toolbar = Actions.createToolStrip();
        IconButton btnRefresh = Actions.asIconButton(new RefreshAction(i18nPas), this);

        IconButton btnFilter = new IconButton();
        btnFilter.setActionType(SelectionType.CHECKBOX);
        btnFilter.setIcon("[SKIN]/actions/filter.png");
        btnFilter.setTitle(i18nPas.DigitalObjectSearchView_FilterButton_Title());
        btnFilter.setTooltip(i18nPas.DigitalObjectSearchView_FilterButton_Hint());
        btnFilter.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (filters.isVisible()) {
                    filters.hide();
                } else {
                    filters.show();
                }
            }
        });

        toolbar.addMember(btnRefresh);
        toolbar.addMember(btnFilter);
        return toolbar;
    }

    private DynamicForm createFilter() {
        DynamicForm form = new DynamicForm();
        form.setBrowserSpellCheck(false);
        form.setValidateOnExit(true);
        form.setSaveOnEnter(true);
        form.setAutoHeight();
        form.setWidth100();
        form.setNumCols(3);

        final RadioGroupItem filterType = new RadioGroupItem(DigitalObjectResourceApi.SEARCH_TYPE_PARAM);
        filterType.setVertical(false);
        filterType.setShowTitle(false);
        filterType.setWrap(false);
        // setRedrawOnChange(true) enforces evaluation of other FormItem.setShowIfCondition
        filterType.setRedrawOnChange(true);
        filterType.setColSpan(2);
        final LinkedHashMap filterMap = new LinkedHashMap();
        filterMap.put(FILTER_LAST_CREATED, i18nPas.DigitalObjectSearchView_FilterGroupLastCreated_Title());
        filterMap.put(FILTER_LAST_MODIFIED, i18nPas.DigitalObjectSearchView_FilterGroupLastModified_Title());
        filterMap.put(FILTER_PHRASE, i18nPas.DigitalObjectSearchView_FilterGroupPhrase_Title());
        filterMap.put(FILTER_QUERY, i18nPas.DigitalObjectSearchView_FilterGroupAdvanced_Title());
        filterType.setValueMap(filterMap);
        filterType.setValue(FILTER_LAST_CREATED);

        FormItemIfFunction showIfAdvanced = new StringMatchFunction(filterType, FILTER_QUERY);
        FormItemIfFunction showIfPhrase = new StringMatchFunction(filterType, FILTER_PHRASE);

        final TextItem phrase = createAdvancedItem(DigitalObjectResourceApi.SEARCH_PHRASE_PARAM,
                i18nPas.DigitalObjectSearchView_FilterPhrase_Title(), showIfPhrase);
        phrase.setValidators(new RequiredIfValidator(new RequiredIfFunction() {

            @Override
            public boolean execute(FormItem formItem, Object value) {
                return FILTER_PHRASE.equals(filterType.getValueAsString());
            }
        }), new LengthRangeValidator() {{
            setMax(1000);
        }});
        
        SubmitItem submit = new SubmitItem("search", i18nPas.DigitalObjectSearchView_FilterSearchButton_Title());

        form.setFields(filterType, new SpacerItem() {{setWidth("100%");}},
                phrase,
                createAdvancedItem(DigitalObjectResourceApi.SEARCH_QUERY_TITLE_PARAM,
                        i18nPas.DigitalObjectSearchView_FilterAdvancedTitle_Title(), showIfAdvanced),
                createAdvancedItem(DigitalObjectResourceApi.SEARCH_QUERY_IDENTIFIER_PARAM,
                        i18nPas.DigitalObjectSearchView_FilterAdvancedIdentifier_Title(), showIfAdvanced),
                createAdvancedItem(DigitalObjectResourceApi.SEARCH_QUERY_LABEL_PARAM,
                        i18nPas.DigitalObjectSearchView_FilterAdvancedLabel_Title(), showIfAdvanced),
                createAdvancedItem(DigitalObjectResourceApi.SEARCH_OWNER_PARAM,
                        i18nPas.DigitalObjectSearchView_FilterAdvancedOwner_Title(), showIfAdvanced),
                createModelItem(i18nPas.DigitalObjectSearchView_FilterAdvancedModel_Title(),
                        new StringMatchFunction(filterType, FILTER_LAST_CREATED, FILTER_LAST_MODIFIED, FILTER_QUERY)),
                submit);
        
        form.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                if (filters.validate(false)) {
                    filter(true);
                }
            }
        });
        return form;
    }

    private static TextItem createAdvancedItem(String name, String title, FormItemIfFunction showIf) {
        TextItem item = new TextItem(name, title);
        if (showIf != null) {
            item.setShowIfCondition(showIf);
        }
        item.setWidth("100%");
        item.setValidators(new LengthRangeValidator() {{ setMax(1000); }});
        return item;
    }

    private static SelectItem createModelItem(String title, FormItemIfFunction showIf) {
        SelectItem item = new SelectItem(DigitalObjectResourceApi.SEARCH_QUERY_MODEL_PARAM, title);
        item.setOptionDataSource(MetaModelDataSource.getInstance());
        item.setAllowEmptyValue(true);
        item.setValueField(MetaModelDataSource.FIELD_PID);
        item.setDisplayField(MetaModelDataSource.FIELD_DISPLAY_NAME);
        item.setAutoFetchData(true);
        item.setValue("model:periodical");
        if (showIf != null) {
            item.setShowIfCondition(showIf);
        }
        return item;
    }

    public Canvas asWidget() {
        return rootWidget;
    }

    public ListGrid getGrid() {
        return foundGrid;
    }

    public void onShow(final boolean select) {
        foundGrid.invalidateCache();
        filter(select);
    }

    @Override
    public SelectedObject[] getSelection() {
        ListGridRecord[] selections = foundGrid.getSelectedRecords();
        SelectedObject[] items = new SelectedObject[selections.length];
        for (int i = 0; i < selections.length; i++) {
            String pid = selections[i].getAttribute(SearchDataSource.FIELD_PID);
            items[i] = new SelectedObject(pid, null);
        }
        return items;
    }

    @Override
    public void refresh() {
        onShow(false);
    }

    private void filter(final boolean select) {
        Criteria valuesAsCriteria = filters.getValuesAsCriteria();
        foundGrid.deselectAllRecords();
        foundGrid.fetchData(valuesAsCriteria, new DSCallback() {

            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (select && RestConfig.isStatusOk(response)) {
                    foundGrid.selectSingleRecord(0);
                }
            }
        });
    }

    private static final class StringMatchFunction implements FormItemIfFunction {

        private final HashSet<String> patterns;
        private final FormItem item2query;

        public StringMatchFunction(FormItem item2query, String... patterns) {
            this.patterns = new HashSet<String>(Arrays.asList(patterns));
            this.item2query = item2query;
        }

        @Override
        public boolean execute(FormItem item, Object value, DynamicForm form) {
            Object itemValue = item2query.getValue();
            return itemValue != null && patterns.contains(itemValue.toString());
        }

    }

}
