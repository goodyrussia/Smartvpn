package com.smartvpn.di

import android.content.Context
import androidx.room.Room
import com.smartvpn.data.local.AppDatabase
import com.smartvpn.data.local.VpnProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "smart_vpn_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideVpnProfileDao(appDatabase: AppDatabase): VpnProfileDao {
        return appDatabase.vpnProfileDao()
    }
}
