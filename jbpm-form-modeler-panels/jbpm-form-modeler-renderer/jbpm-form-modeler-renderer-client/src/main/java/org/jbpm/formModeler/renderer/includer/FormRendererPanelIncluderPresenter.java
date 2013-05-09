/**
 * Copyright (C) 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.formModeler.renderer.includer;

import org.jboss.errai.bus.client.api.RemoteCallback;
import org.jboss.errai.bus.client.framework.MessageBus;
import org.jboss.errai.ioc.client.api.Caller;
import org.jbpm.formModeler.api.events.FormRenderEvent;
import org.jbpm.formModeler.api.events.FormSubmitFailEvent;
import org.jbpm.formModeler.api.events.FormSubmittedEvent;
import org.jbpm.formModeler.api.model.FormTO;
import org.jbpm.formModeler.api.processing.FormRenderContextTO;
import org.jbpm.formModeler.api.processing.FormRenderListener;
import org.jbpm.formModeler.api.processing.FormRenderContext;
import org.jbpm.formModeler.api.processing.FormRenderContextTO;
import org.jbpm.formModeler.api.processing.FormRenderListener;
import org.jbpm.formModeler.renderer.service.FormRenderingService;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.mvp.UberView;
import org.uberfire.client.workbench.widgets.events.NotificationEvent;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;


@Dependent
@WorkbenchScreen(identifier = "FormRendererIncluderPanel")
public class FormRendererPanelIncluderPresenter {

    @Inject
    MessageBus bus;

    @Inject
    Event<FormRenderEvent> formRenderEvent;

    private FormRenderContextTO context;

    public interface FormRendererIncluderPanelView
            extends
            UberView<FormRendererPanelIncluderPresenter> {

        void addForms(List<FormTO> forms);
        void loadContext(FormRenderContextTO ctx);
    }

    @Inject
    private PlaceManager placeManager;

    @Inject
    FormRendererIncluderPanelView view;

    @Inject
    private Event<NotificationEvent> notification;

    @Inject
    Caller<FormRenderingService> renderingService;

    @PostConstruct
    public void init() {
        renderingService.call(new RemoteCallback<List<FormTO>>() {
            @Override
            public void callback(List<FormTO> forms) {
                view.addForms(forms);
            }

        }).getAllForms();
    }

    public void loadForm(Long formId) {
        renderingService.call(new RemoteCallback<FormRenderContextTO>() {
            @Override
            public void callback(FormRenderContextTO ctx) {
                view.loadContext(ctx);
            }
        }).startRendering(formId, new HashMap<String, Object>(), new FormRenderListener());
    }

    public void startTest() {
        renderingService.call(new RemoteCallback<FormRenderContextTO>() {
            @Override
            public void callback(FormRenderContextTO ctx) {
                context = ctx;
                view.loadContext(ctx);
            }
        }).launchTest();
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return "Form Renderer Panel Includer";
    }

    @WorkbenchPartView
    public UberView<FormRendererPanelIncluderPresenter> getView() {
        return view;
    }

    public void notifyErrors(int errorNumber) {
        notification.fire(new NotificationEvent("Unable to process form, it has " + errorNumber + " errors!"));
    }

    public void notifyFormSubmit() {
        notification.fire(new NotificationEvent("Form submitted OK!"));
    }

    public void notifyFormProcessingError(String cause) {
        notification.fire(new NotificationEvent("Something wrong happened processing form, cause: '" + cause + "'"));
    }

    //Event Observers
    public void onFormSubmitted(@Observes FormSubmittedEvent event) {
        if (event.isMine(context)) {
            int errors = event.getContext().getErrors();
            if (errors == 0) {
                notifyFormSubmit();
            } else {
                notifyErrors(errors);
            }
        }
    }

    public  void onFormSubmitFail(@Observes FormSubmitFailEvent event) {
        if (event.isMine(context)) {
            notifyFormProcessingError(event.getCause());
        }
    }
}