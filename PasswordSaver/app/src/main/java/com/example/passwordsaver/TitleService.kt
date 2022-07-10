package com.example.passwordsaver

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class TitleService : TileService() {
    override fun onClick() {
        super.onClick()
        val tile = qsTile
        tile.state = Tile.STATE_ACTIVE
        val intent = Intent(this.applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("copy", true)
        applicationContext.startActivity(intent)
        tile.updateTile()
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

}