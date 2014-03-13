/*
 * Copyright (C) 2011 Jan Pokorsky
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
package cz.cas.lib.proarc.webapp.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.smartgwt.client.core.Function;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ClickMaskMode;
import com.smartgwt.client.types.DateDisplayFormat;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.util.DateUtil;
import com.smartgwt.client.util.I18nUtil;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IconButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.IconMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ItemClickEvent;
import com.smartgwt.client.widgets.menu.events.ItemClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.FolderClosedEvent;
import com.smartgwt.client.widgets.tree.events.FolderClosedHandler;
import com.smartgwt.client.widgets.tree.events.LeafClickEvent;
import com.smartgwt.client.widgets.tree.events.LeafClickHandler;
import cz.cas.lib.proarc.webapp.client.ClientUtils.SweepTask;
import cz.cas.lib.proarc.webapp.client.action.AbstractAction;
import cz.cas.lib.proarc.webapp.client.action.ActionEvent;
import cz.cas.lib.proarc.webapp.client.action.Actions;
import cz.cas.lib.proarc.webapp.client.ds.LanguagesDataSource;
import cz.cas.lib.proarc.webapp.client.ds.LocalizationDataSource;
import cz.cas.lib.proarc.webapp.client.ds.RestConfig;
import cz.cas.lib.proarc.webapp.client.ds.UserDataSource;
import cz.cas.lib.proarc.webapp.client.ds.UserPermissionDataSource;
import cz.cas.lib.proarc.webapp.client.ds.ValueMapDataSource;
import cz.cas.lib.proarc.webapp.client.presenter.DeviceManager;
import cz.cas.lib.proarc.webapp.client.presenter.DeviceManaging.DeviceManagerPlace;
import cz.cas.lib.proarc.webapp.client.presenter.DigitalObjectCreating.DigitalObjectCreatorPlace;
import cz.cas.lib.proarc.webapp.client.presenter.DigitalObjectCreator;
import cz.cas.lib.proarc.webapp.client.presenter.DigitalObjectEditor;
import cz.cas.lib.proarc.webapp.client.presenter.DigitalObjectManager;
import cz.cas.lib.proarc.webapp.client.presenter.DigitalObjectManaging.DigitalObjectManagerPlace;
import cz.cas.lib.proarc.webapp.client.presenter.ImportPresenter;
import cz.cas.lib.proarc.webapp.client.presenter.Importing.ImportPlace;
import cz.cas.lib.proarc.webapp.client.presenter.Importing.ImportPlace.Type;
import cz.cas.lib.proarc.webapp.client.presenter.UserManaging.UsersPlace;
import cz.cas.lib.proarc.webapp.client.widget.AboutWindow;
import cz.cas.lib.proarc.webapp.client.widget.LoginWindow;
import cz.cas.lib.proarc.webapp.client.widget.UserInfoView;
import cz.cas.lib.proarc.webapp.client.widget.UsersView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Editor implements EntryPoint {

    public static final String CSS_PANEL_DESCRIPTION_TITLE = "proarcPanelDescriptionTitle";

    private static final Logger LOG = Logger.getLogger(Editor.class.getName());
    
    /** {@link TreeNode } attribute to associate the node with {@link Place}. */
    private static final String PLACE_ATTRIBUTE = "GoToPlace";
    private static final String LOCALE_ATTRIBUTE = "locale";
    private static final String GLOBAL_MENU_CONTAINER_ID = "proarcGlobalMenuContainer";
    private static Editor INSTANCE;

    private ClientMessages i18n;
    private PresenterFactory presenterFactory;
    private final HashSet<String> permissions = new HashSet<String>();
    private EditorWorkFlow editorWorkFlow;
    private Layout editorDisplay;
    private SweepTask sweepTask;
    private ErrorHandler errorHandler;
    private Record user;

    public static Editor getInstance() {
        return INSTANCE;
    }

    public EditorWorkFlow getEditorWorkFlow() {
        return editorWorkFlow;
    }

    public PresenterFactory getPresenterFactory() {
        return presenterFactory;
    }

    public ErrorHandler getTransportErrorHandler() {
        return errorHandler;
    }

    public Record getUser() {
        return user;
    }

    @Override
    public void onModuleLoad() {
        INSTANCE = this;
        initLogging();
        
        ClientUtils.info(LOG, "onModuleLoad:\n module page: %s\n host page: %s"
                + "\n getModuleName: %s\n getPermutationStrongName: %s\n version: %s"
                + "\n Page.getAppDir: %s, \n Locale: %s",
                GWT.getModuleBaseURL(), GWT.getHostPageBaseURL(),
                GWT.getModuleName(), GWT.getPermutationStrongName(), GWT.getVersion(),
                Page.getAppDir(), LanguagesDataSource.activeLocale()
                );

        // replace default DateDisplayFormat.TOUSSHORTDATE
        DateUtil.setShortDateDisplayFormat(DateDisplayFormat.TOEUROPEANSHORTDATE);
        DateUtil.setShortDatetimeDisplayFormat(DateDisplayFormat.TOEUROPEANSHORTDATETIME);

        I18nUtil.initMessages(ClientUtils.createSmartGwtMessages());
        i18n = GWT.create(ClientMessages.class);

        errorHandler = new ErrorHandler();
        errorHandler.initTransportErrorHandler();

        LoginWindow.login();

        presenterFactory = new PresenterFactory(i18n);

        editorWorkFlow = new EditorWorkFlow(getDisplay(), presenterFactory, i18n);
        presenterFactory.setPlaceController(editorWorkFlow.getPlaceController());

        final TreeGrid menu = createMenu();

        sweepTask = new SweepTask() {

            @Override
            public void processing() {
                SC.clearPrompt();
                TreeNode[] menuContent = createMenuContent();
                Tree tree = menu.getTree();
                TreeNode root = tree.getRoot();
                tree.addList(menuContent, root);
                tree.openAll();
                editorWorkFlow.init();
            }
        };

        final HLayout mainLayout = new HLayout();
//        mainLayout.setLayoutMargin(5);
        mainLayout.setWidth100();
        mainLayout.setHeight100();
        mainLayout.setMembers(getDisplay());

        Canvas mainHeader = createMainHeader(menu);

        VLayout desktop = new VLayout(0);
        desktop.setWidth100();
        desktop.setHeight100();
        desktop.setMembers(mainHeader, mainLayout);
        desktop.draw();

        SC.showPrompt(i18n.Editor_InitialLoading_Msg());
        LocalizationDataSource.getInstance().initOnStart(sweepTask.expect());
        ValueMapDataSource.getInstance().initOnStart(sweepTask.expect());
        loadPermissions();
    }

    private void loadPermissions() {
        sweepTask.expect();
        UserPermissionDataSource.getInstance().fetchData(null, new DSCallback() {

            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (RestConfig.isStatusOk(response)) {
                    Record[] data = response.getData();
                    permissions.clear();
                    permissions.addAll(UserPermissionDataSource.asPermissions(data));
                    sweepTask.release();
                }
            }
        });
    }

    /**
     * Gets container to display editor activities.
     */
    private Layout getDisplay() {
        if (editorDisplay == null) {
            editorDisplay = new HLayout();
            editorDisplay.setHeight100();
            editorDisplay.setWidth100();
        }
        return editorDisplay;
    }

    private Canvas createMainHeader(TreeGrid menu) {
        ToolStrip mainHeader = new ToolStrip();
        mainHeader.setWidth100();
        mainHeader.setHeight(40);

        mainHeader.addSpacer(6);

        Label headerItem = new Label();
        headerItem.setWrap(false);
        headerItem.setIcon("16/logo.png");
        headerItem.setIconHeight(16);
        headerItem.setIconWidth(205);
//        headerItem.setIcon("24/logo.png");
//        headerItem.setIconHeight(24);
//        headerItem.setIconWidth(307);
//        headerItem.setIcon("20/logo.png");
//        headerItem.setIconHeight(20);
//        headerItem.setIconWidth(256);

        mainHeader.addMember(createGlobalMenuButton(menu));
        mainHeader.addMember(createLangMenu());
        createUserLink(mainHeader, mainHeader.getMembers().length);
        mainHeader.addFill();
        mainHeader.addMember(headerItem);
        mainHeader.addSpacer(6);

        return mainHeader;
    }

    private Canvas createLangMenu() {
        String activeLocale = LanguagesDataSource.activeLocale();
        IconMenuButton langMenuButton = Actions.asIconMenuButton(Actions.emptyAction(activeLocale, null, null), this);
        langMenuButton.setCanFocus(Boolean.FALSE);
        Menu m = new Menu();
        m.setShowShadow(Boolean.TRUE);
        m.addItem(createLangItem("cs", "Česky", activeLocale));
        m.addItem(createLangItem("en", "English", activeLocale));
        langMenuButton.setMenu(m);
        m.addItemClickHandler(new ItemClickHandler() {

            @Override
            public void onItemClick(ItemClickEvent event) {
                MenuItem item = event.getItem();
                if (!item.getChecked()) {
                    switchLocale(item.getAttribute(LOCALE_ATTRIBUTE));
                }
            }
        });

        Record rec = m.getDataAsRecordList().find(LOCALE_ATTRIBUTE, activeLocale);
        langMenuButton.setTitle(rec.getAttribute("title"));
        return langMenuButton;
    }

    private MenuItem createLangItem(String locale, String title, String activeLocale) {
        MenuItem mi = new MenuItem(title);
        mi.setAttribute(LOCALE_ATTRIBUTE, locale);
        if (locale.equals(activeLocale)) {
            mi.setChecked(Boolean.TRUE);
        }
        return mi;
    }

    /**
     * Switches the application locale.
     * @param locale new locale
     */
    private void switchLocale(String locale) {
        UrlBuilder urlBuilder = Window.Location.createUrlBuilder();
        urlBuilder.setParameter(LOCALE_ATTRIBUTE, locale);
        String url = urlBuilder.buildString();
        Window.Location.assign(url);
    }

    private Canvas createUserLink(final ToolStrip mainHeader, final int index) {
        final UserInfoView userInfoView = new UserInfoView(i18n);
        final IconButton userButtonRef[] = new IconButton[1];
        final IconButton userButton = Actions.asIconButton(new AbstractAction(null, null, null) {
            @Override
            public void performAction(ActionEvent event) {
                userInfoView.show(userButtonRef[0], "bottom", true);
            }
        }, this);
        userButtonRef[0] = userButton;
        userButton.setCanFocus(Boolean.FALSE);

        UserDataSource.getInstance().fetchData(new Criteria(UserDataSource.FIELD_WHOAMI, "true"), new DSCallback() {

            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                String title = "Unknown user";
                if (response.getStatus() == DSResponse.STATUS_SUCCESS) {
                    Record[] data = response.getData();
                    if (data.length > 0) {
                        user = data[0];
                        title = user.getAttribute(UserDataSource.FIELD_USERNAME);
                    }
                }
                userButton.setTitle(title);
                mainHeader.addMember(userButton, index);
            }
        });
        return userButton;
    }

    private Canvas createGlobalMenuButton(final TreeGrid menu) {
        // Render the menu as HLayout instead of Window to get rid of edges introduced by SmartGWT 4.0
        final HLayout menuWindow = new HLayout();
        menuWindow.setID(GLOBAL_MENU_CONTAINER_ID);
        menuWindow.setShowShadow(true);
        menuWindow.setWidth(200);
        menuWindow.setMembers(menu);
        final IconMenuButton[] globalMenuButton = new IconMenuButton[1];
        globalMenuButton[0] = Actions.asIconMenuButton(new AbstractAction(
                i18n.MainMenu_Title(), null, null) {

            @Override
            public void performAction(ActionEvent event) {
                menuWindow.showNextTo(globalMenuButton[0], "bottom", true);
                menuWindow.showClickMask(new Function() {

                    @Override
                    public void execute() {
                        menuWindow.hide();
                    }
                }, ClickMaskMode.SOFT, new Canvas[] {menuWindow});
                menu.focus();
            }
        }, new Object());
        globalMenuButton[0].setAutoWidth();
        globalMenuButton[0].setCanFocus(Boolean.FALSE);
        return globalMenuButton[0];
    }

    private TreeNode[] createMenuContent() {
        TreeNode[] trees = new TreeNode[] {
                createTreeNode("Import", i18n.MainMenu_Import_Title(),
                        createTreeNode("New Batch", i18n.MainMenu_Import_NewBatch_Title(), new ImportPlace(Type.CONTENT)),
                        createTreeNode("History", i18n.MainMenu_Import_Edit_Title(), new ImportPlace(Type.HISTORY))),
                createTreeNode("Edit", i18n.MainMenu_Edit_Title(),
                        createTreeNode("New Object", i18n.MainMenu_Edit_NewObject_Title(), new DigitalObjectCreatorPlace()),
                        createTreeNode("Search", i18n.MainMenu_Edit_Edit_Title(), new DigitalObjectManagerPlace())
                ),
                createTreeNode("Devices", i18n.MainMenu_Devices_Title(), new DeviceManagerPlace()),
//                createTreeNode("Statistics", i18n.MainMenu_Statistics_Title()),
                createProtectedTreeNode("Users", i18n.MainMenu_Users_Title(), new UsersPlace(), Arrays.asList("proarc.permission.admin")),
                createProtectedTreeNode("Console", i18n.MainMenu_Console_Title(), Arrays.asList("proarc.permission.admin")),
                createTreeNode("About", i18n.AboutWindow_Title()),
        };
        trees = reduce(trees);
        for (int i = 0; i < trees.length; i++) {
            TreeNode treeNode = trees[i];
            ClientUtils.fine(LOG, "TreeNode.array: %s, i: %s", treeNode.getName(), i);
        }
        return trees;
    }

    private TreeGrid createMenu() {
        final TreeGrid menu = new TreeGrid();
        menu.setAutoFitData(Autofit.VERTICAL);
        menu.setLeaveScrollbarGap(false);

        menu.setShowHeader(false);
        menu.setShowOpener(false);
        menu.setShowOpenIcons(false);
        menu.setCanCollapseGroup(false);
        menu.setSelectionType(SelectionStyle.NONE);
        menu.addFolderClosedHandler(new FolderClosedHandler() {

            @Override
            public void onFolderClosed(FolderClosedEvent event) {
                event.cancel();
            }
        });
        menu.addLeafClickHandler(new LeafClickHandler() {

            @Override
            public void onLeafClick(LeafClickEvent event) {
                for (Canvas parent = menu.getParentElement(); parent != null; parent = parent.getParentElement()) {
                    if (GLOBAL_MENU_CONTAINER_ID.equals(parent.getID())) {
                        parent.hide();
                        break;
                    }
                }
                ClientUtils.fine(LOG, "menu.getSelectedPaths: %s\nmenu.getSelectedRecord: %s",
                        menu.getSelectedPaths(), menu.getSelectedRecord());
                String name = event.getLeaf().getName();
                final PlaceController placeController = getEditorWorkFlow().getPlaceController();
                Object placeObj = event.getLeaf().getAttributeAsObject(PLACE_ATTRIBUTE);
                if (placeObj instanceof Place) {
                    placeController.goTo((Place) placeObj);
                    return ;
                }
                if ("Console".equals(name)) {
                    SC.showConsole();
                } else if ("About".equals(name)) {
                    AboutWindow.getInstance(i18n).show();
                } else {
                    Layout placesContainer = getDisplay();
                    placesContainer.removeMembers(placesContainer.getMembers());
                }
            }
        });
        return menu;
    }

    private boolean checkCredentials(List<String> requires) {
        if (requires != null && !permissions.containsAll(requires)) {
            return true;
        }
        return false;
    }

    private TreeNode createProtectedTreeNode(String name, String displayName, Place place, List<String> requires) {
        if (checkCredentials(requires)) {
            return null;
        }
        return createTreeNode(name, displayName, place, (TreeNode[]) null);
    }

    private TreeNode createProtectedTreeNode(String name, String displayName,
            List<String> requires, TreeNode... children) {

        if (checkCredentials(requires)) {
            return null;
        }
        return createTreeNode(name, displayName, null, children);
    }

    private TreeNode createTreeNode(String name, String displayName, TreeNode... children) {
        return createTreeNode(name, displayName, null, children);
    }

    private TreeNode createTreeNode(String name, String displayName, Place place, TreeNode... children) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        TreeNode treeNode = new TreeNode(name);
        if (displayName != null) {
            treeNode.setTitle(displayName);
        }
        if (children != null && children.length > 0) {
            treeNode.setChildren(children);
        }
        if (place != null) {
            treeNode.setAttribute(PLACE_ATTRIBUTE, place);
        }
        return treeNode;
    }

    private static TreeNode[] reduce(TreeNode[] nodes) {
        ArrayList<TreeNode> result = new ArrayList<TreeNode>(nodes.length);
        for (TreeNode treeNode : nodes) {
            if (treeNode != null) {
                result.add(treeNode);
            }
        }
        return result.toArray(new TreeNode[result.size()]);
    }

    private void initLogging() {
        Dictionary levels = Dictionary.getDictionary("EditorLoggingConfiguration");
        for (String loggerName : levels.keySet()) {
            String levelValue = levels.get(loggerName);
            try {
                Level level = Level.parse(levelValue);
                Logger logger = Logger.getLogger(loggerName);
                logger.setLevel(level);
                Logger.getLogger("").info(ClientUtils.format(
                        "logger: '%s', levelValue: %s", loggerName, level));
            } catch (IllegalArgumentException ex) {
                Logger.getLogger("").log(Level.SEVERE,
                        ClientUtils.format("logger: '%s', levelValue: %s", loggerName, levelValue), ex);
            }
        }

        if (GWT.isProdMode()) {
            // XXX SmartGWT 3.0 ignores thrown exceptions in production mode.
            // Javascript stack traces are useless but messages can be valuable
            GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {

                @Override
                public void onUncaughtException(Throwable e) {
                    StringBuilder sb = new StringBuilder();
                    for (Throwable t = e; t != null; t = t.getCause()) {
                        sb.append("* ").append(t.getClass().getName()).append(": ")
                                .append(t.getLocalizedMessage()).append("\n");
                        for (StackTraceElement elm : t.getStackTrace()) {
                            sb.append("  ").append(elm.toString()).append("\n");
                        }
                    }

                    // visible in javascript console; Window.alert is too much intrusive.
                    LOG.log(Level.SEVERE, e.getMessage(), e);
//                    Window.alert(sb.toString());
                }
            });
        }
    }

    public static final class PresenterFactory {

        private ImportPresenter importPresenter;
        private DeviceManager deviceManager;
        private DigitalObjectCreator digitalObjectCreator;
        private DigitalObjectEditor digitalObjectEditor;
        private DigitalObjectManager digitalObjectManager;
        private UsersView users;
        private final ClientMessages i18n;
        private PlaceController placeController;

        PresenterFactory(ClientMessages i18n) {
            this.i18n = i18n;
        }

        void setPlaceController(PlaceController placeController) {
            this.placeController = placeController;
        }

        public ImportPresenter getImportPresenter() {
            if (importPresenter == null) {
                importPresenter = new ImportPresenter(i18n, placeController);
            }
            return importPresenter;
        }

        public DeviceManager getDeviceManager() {
            if (deviceManager == null) {
                deviceManager = new DeviceManager(i18n, placeController);
            }
            return deviceManager;
        }

        public DigitalObjectCreator getDigitalObjectCreator() {
            if (digitalObjectCreator == null) {
                digitalObjectCreator = new DigitalObjectCreator(i18n, placeController);
            }
            return digitalObjectCreator;
        }

        public DigitalObjectEditor getDigitalObjectEditor() {
            if (digitalObjectEditor == null) {
                digitalObjectEditor = new DigitalObjectEditor(i18n, placeController);
            }
            return digitalObjectEditor;
        }

        public DigitalObjectManager getDigitalObjectManager() {
            if (digitalObjectManager == null) {
                digitalObjectManager = new DigitalObjectManager(i18n, placeController);
            }
            return digitalObjectManager;
        }

        public UsersView getUsers() {
            if (users == null) {
                users = new UsersView(i18n);
            }
            return users;
        }

    }

}
