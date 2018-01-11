/*-
 * <<
 * UAVStack
 * ==
 * Copyright (C) 2016 - 2017 UAVStack
 * ==
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * >>
 */

package com.creditease.uav.hook.redis.jedis;

import java.util.Map;

import com.creditease.monitor.appfra.hook.spi.HookConstants;
import com.creditease.monitor.appfra.hook.spi.HookContext;
import com.creditease.monitor.appfra.hook.spi.HookProxy;
import com.creditease.monitor.interceptframework.spi.InterceptConstants;
import com.creditease.monitor.interceptframework.spi.InterceptContext;
import com.creditease.monitor.interceptframework.spi.InterceptContext.Event;
import com.creditease.uav.monitorframework.dproxy.DynamicProxyInstaller;
import com.creditease.uav.monitorframework.dproxy.DynamicProxyProcessor;
import com.creditease.uav.monitorframework.dproxy.bytecode.DPClass;
import com.creditease.uav.monitorframework.dproxy.bytecode.DPMethod;
import com.creditease.uav.util.MonitorServerUtil;

public class JedisHookProxy extends HookProxy {

    private DynamicProxyInstaller dpInstaller;

    public JedisHookProxy(String id, @SuppressWarnings("rawtypes") Map config) {
        super(id, config);
        dpInstaller = new DynamicProxyInstaller();
    }

    @Override
    public void start(HookContext context, ClassLoader webapploader) {

        Event evt = context.get(Event.class);

        switch (evt) {
            case SPRING_BEAN_REGIST:
            case WEBCONTAINER_INIT:
                insertIntercepter(context, webapploader);
                break;
            // when servlet init
            case AFTER_SERVET_INIT:
                break;
            case BEFORE_SERVLET_DESTROY:
                break;
            case WEBCONTAINER_STARTED:
                break;
            case WEBCONTAINER_STOPPED:
                break;
            default:
                break;
        }
    }

    @Override
    public void stop(HookContext context, ClassLoader webapploader) {

        Event evt = context.get(Event.class);

        switch (evt) {
            // when servlet init
            case AFTER_SERVET_INIT:
                break;
            case BEFORE_SERVLET_DESTROY:
                break;
            case WEBCONTAINER_STARTED:

                break;
            case WEBCONTAINER_STOPPED:
                break;
            default:
                break;
        }
    }

    protected void insertIntercepter(HookContext context, ClassLoader webapploader) {

        if (isHookEventDone("insertIntercepter")) {
            return;
        }

        InterceptContext ic = (InterceptContext) context.get(HookConstants.INTERCEPTCONTEXT);

        String contextPath = (String) ic.get(InterceptConstants.CONTEXTPATH);
        String basePath = (String) ic.get(InterceptConstants.BASEPATH);

        final String appid = MonitorServerUtil.getApplicationId(contextPath, basePath);

        doProxyInstall(webapploader, appid);
    }

    public void doProxyInstall(ClassLoader webapploader, final String appid) {

        /**
         * set the webapploader is the target classloader
         */
        dpInstaller.setTargetClassLoader(webapploader);

        dpInstaller.installProxy("redis.clients.jedis.Connection",
                new String[] { "com.creditease.uav.hook.redis.jedis.interceptors" }, new DynamicProxyProcessor() {

                    @Override
                    public void process(DPMethod m) throws Exception {

                        if ("sendCommand".equals(m.getName())) {
                            DPClass[] ccs = m.getParameterTypes();
                            if (ccs.length == 2 && "byte[][]".equals(ccs[1].getName())) {
                                m.insertBefore("{JedisConnectionIT.start(\"" + appid
                                        + "\", new Object[]{$1, $2, getHost(), new Integer(getPort())});}");
                                m.insertAfter("{JedisConnectionIT.end(new Object[]{$_});}");

                                dpInstaller.addCatch(m, "JedisConnectionIT.end(new Object[]{$e});");
                            }
                        }
                    }

                }, false);
    }
}
