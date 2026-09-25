package com.sfs.adapters.spi;

public interface FileTypeAdapter {

    AdapterDescriptor descriptor();

    AdapterResult adapt(AdapterRequest request);
}
