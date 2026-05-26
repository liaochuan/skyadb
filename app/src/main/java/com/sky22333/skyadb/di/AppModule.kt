package com.sky22333.skyadb.di

import com.sky22333.skyadb.adb.KadbManager
import com.sky22333.skyadb.data.AppSettingsStore
import com.sky22333.skyadb.data.RecentDeviceStore
import com.sky22333.skyadb.download.NetworkDownloadManager
import com.sky22333.skyadb.files.LocalFileManager
import com.sky22333.skyadb.repository.AdbRepository
import com.sky22333.skyadb.repository.DefaultAdbRepository
import org.koin.dsl.module

val appModule = module {
    single { KadbManager() }
    single { NetworkDownloadManager(get()) }
    single { LocalFileManager(get()) }
    single { AppSettingsStore(get()) }
    single { RecentDeviceStore(get()) }
    single<AdbRepository> { DefaultAdbRepository(get(), get(), get()) }
}
