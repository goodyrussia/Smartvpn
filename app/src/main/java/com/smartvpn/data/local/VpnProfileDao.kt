package com.smartvpn.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnProfileDao {
    @Query("SELECT * FROM vpn_profiles")
    fun getAllProfiles(): Flow<List<VpnProfile>>

    @Query("SELECT * FROM vpn_profiles WHERE id = :profileId")
    suspend fun getProfileById(profileId: Int): VpnProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VpnProfile)

    @Update
    suspend fun updateProfile(profile: VpnProfile)

    @Delete
    suspend fun deleteProfile(profile: VpnProfile)
}
