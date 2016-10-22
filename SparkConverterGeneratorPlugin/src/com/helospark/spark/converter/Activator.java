package com.helospark.spark.converter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.helospark.spark.converter.handlers.DefaultCompilationUnitProvider;
import com.helospark.spark.converter.handlers.InputParameterGetter;
import com.helospark.spark.converter.handlers.service.ClassTypeAppender;
import com.helospark.spark.converter.handlers.service.CompilationUnitCreator;
import com.helospark.spark.converter.handlers.service.CompilationUnitParser;
import com.helospark.spark.converter.handlers.service.ConverterClassGenerator;
import com.helospark.spark.converter.handlers.service.ConverterGenerator;
import com.helospark.spark.converter.handlers.service.PackageRootFinder;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
    public static List<Object> diContainer = new ArrayList<>();
    // The plug-in ID
    public static final String PLUGIN_ID = "com.helospark.SparkConverterGenerator";

    // The shared instance
    private static Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
     * BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        initializeDiContainer();
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
     * BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path
     *
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    private void initializeDiContainer() {
        diContainer.add(new DefaultCompilationUnitProvider());
        diContainer.add(new InputParameterGetter(getDependency(DefaultCompilationUnitProvider.class)));
        diContainer.add(new PackageRootFinder());
        diContainer.add(new CompilationUnitCreator());
        diContainer.add(new CompilationUnitParser());
        diContainer.add(new ClassTypeAppender());
        diContainer.add(new ConverterClassGenerator());
        diContainer.add(new ConverterGenerator(getDependency(PackageRootFinder.class),
                getDependency(CompilationUnitCreator.class), getDependency(CompilationUnitParser.class),
                getDependency(ClassTypeAppender.class), getDependency(ConverterClassGenerator.class)));
    }

    /**
     * Probably will be deprecated after I will be able to create e4 plugin.
     * 
     * @param clazz
     *            type to get
     * @return dependency of that class
     */
    @SuppressWarnings("unchecked")
    public static <T> T getDependency(Class<T> clazz) {
        return (T) diContainer.stream()
                .filter(value -> value.getClass().equals(clazz))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to initialize"));
    }
}