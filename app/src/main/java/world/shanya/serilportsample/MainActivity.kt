package world.shanya.serilportsample

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.akexorcist.snaptimepicker.SnapTimePickerDialog
import com.akexorcist.snaptimepicker.TimeRange
import com.akexorcist.snaptimepicker.TimeValue
import com.gcssloop.widget.ArcSeekBar
import com.gcssloop.widget.ArcSeekBar.OnProgressChangeListener
import kotlinx.android.synthetic.main.activity_main.*
import world.shanya.serialport.SerialPort
import world.shanya.serialport.SerialPortBuilder
import world.shanya.serialport.discovery.DiscoveryActivity


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 串口初始化
        val serialPort = SerialPortBuilder
            .isDebug(true)
            .autoConnect(true)
            .setReadDataType(SerialPort.READ_HEX)
            .setSendDataType(SerialPort.SEND_HEX)
            .setReceivedDataListener {
                Log.d("SerialPortDebug", "received: ${it}")
            }
            .build(this)

        // 连接
        serialPort.setConnectStatusCallback { status, device ->
            if (status) {
                Log.d("SerialPortDebug", "连接: ${device.address}")


                // 电流
                Handler(Looper.getMainLooper()).postDelayed({
                    serialPort.sendData(
                        "EE1000000000FE"
                    )

                }, 300)

                Handler(Looper.getMainLooper()).postDelayed({
                    serialPort.sendData(
                        "EE1000000000FE"
                    )
                    switchButton.isChecked = true
                }, 500)

                // 电池
                Handler(Looper.getMainLooper()).postDelayed({
                    serialPort.sendData(
                        "EE1100000000FF"
                    )
                }, 1000)

            } else {
                Log.d("SerialPortDebug", "断开")
            }
        }

        // 开关
        switchButton.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                serialPort.sendData(
                    "EE0100000000EF"
                )
            } else {
                serialPort.sendData(
                    "EE0200000000F0"
                )
            }
        }


        // 连接按钮
        buttonConnect.setOnClickListener {
            serialPort.openDiscoveryActivity(Intent(this, DiscoveryActivity::class.java))
        }
        buttonDisconnect.setOnClickListener {
            serialPort.disconnect()
        }

        // 数码管字体
        val face = Typeface.createFromAsset(
            assets,
            "digital.ttf"
        )
        textTimer.setTypeface(face)
        textCur.setTypeface(face)
        text15Vol.setTypeface(face)
        textGrade.setTypeface(face)
        button9V.setTypeface(face)
        // 电池
        button9V.setOnClickListener(View.OnClickListener {
            serialPort.sendData(
                "EE1100000000FF"
            )
        })

        fun setTimer(hour: Int, minute: Int) {
            val cdt = MyCountDownTimer(
                ((hour * 60) + minute) * 60000.toLong(),
                1000,
                textTimer,
                serialPort
            )
            cdt.start()
            buttonTimer.text = "取消"
            buttonTimer.setOnClickListener {
                cdt.cancel()
                textTimer.text = "00:00"
                buttonTimer.text = "定时"
                buttonTimer.setOnClickListener {
                    SnapTimePickerDialog.Builder().apply {
                        setPreselectedTime(TimeValue(0, 20))
                        setSelectableTimeRange(TimeRange(TimeValue(0, 1), TimeValue(2, 0)))
                    }.build().apply {
                        setListener { hour, minute ->
                            Log.d(hour.toString(), minute.toString())
                            setTimer(hour, minute)
                        }
                    }.show(supportFragmentManager, SnapTimePickerDialog.TAG)
                }
            }
        }

        buttonTimer.setOnClickListener {
            SnapTimePickerDialog.Builder().apply {
                setPreselectedTime(TimeValue(0, 20))
                setSelectableTimeRange(TimeRange(TimeValue(0, 1), TimeValue(2, 0)))
            }.build().apply {
                setListener { hour, minute ->
                    Log.d(hour.toString(), minute.toString())
                    setTimer(hour, minute)
                }
            }.show(supportFragmentManager, SnapTimePickerDialog.TAG)
        }


        // 电流
        buttonRA.setOnClickListener {
            serialPort.sendData(
                "EE1000000000FE"
            )
        }

        // 旋钮
        var seek_temp = 0
        arc_seek_bar.setOnProgressChangeListener(object : OnProgressChangeListener {
            override fun onProgressChanged(seekBar: ArcSeekBar, progress: Int, isUser: Boolean) {
                // 减少到0
                when {
                    progress == 0 -> {
                        CurZero(serialPort, textCur)
                    }
                    // 减小电流
                    progress < seek_temp -> {
                        serialPort.sendData(
                            "EE0400000000F2"
                        )
                        // 300ms后读取电流
                        Handler(Looper.getMainLooper()).postDelayed({
                            serialPort.sendData(
                                "EE1000000000FE"
                            )
                        }, 300)
                    }
                    // 增加电流
                    progress > seek_temp -> {
                        serialPort.sendData(
                            "EE0300000000F1"
                        )
                        // 300ms后读取电流
                        Handler(Looper.getMainLooper()).postDelayed({
                            serialPort.sendData(
                                "EE1000000000FE"
                            )
                        }, 300)

                    }
                }

                // 更新等级
                textGrade.text = progress.toString()
                seek_temp = progress
                Log.d("旋钮", progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: ArcSeekBar) {}
            override fun onStopTrackingTouch(seekBar: ArcSeekBar) {}
        })


        serialPort.setReceivedDataListener {
            // it即为接收数据
            Log.d("SerialPortDebug", "received: ${it}")
            when {
                it.startsWith("EE  1") -> {
                    Toast.makeText(this, "已开机", Toast.LENGTH_SHORT).show()
                    switchButton.isChecked = true
                }
                it.startsWith("EE  2") -> {
                    Toast.makeText(this, "已关机", Toast.LENGTH_SHORT).show()
                    switchButton.isChecked = false
                    textCur.text = "0.00 mA"
                }

                it.startsWith("EE  3") -> {
                    Toast.makeText(this, "电流已增加", Toast.LENGTH_SHORT).show()
                    serialPort.sendData(
                        "EE1000000000FE"
                    )
                }
                it.startsWith("EE  4") -> {
                    Toast.makeText(this, "电流已降低", Toast.LENGTH_SHORT).show()
                    serialPort.sendData(
                        "EE1000000000FE"
                    )
                }
                it.startsWith("EF 10") ->
                    textCur.text = InfoFormat(it) + " mA"
                it.startsWith("EF 11") ->
                    button9V.text = InfoFormat(it) + " V"
            }

        }
    }

    fun InfoFormat(str: String): String {
        var raw = "0.0"
        try {
            raw =
                (str.substring(9, 11) + str.substring(6, 8))
                    .replace(" ", "").toInt(16).toString()
            raw = (if (raw.substring(0, raw.length - 2) == "") "0" else raw.substring(
                0,
                raw.length - 2
            )) + "." + raw.substring(raw.length - 2)
        } finally {
            return raw
        }
    }

    fun CurZero(serialPort: SerialPort, textCur: TextView) {
        serialPort.sendData(
            "EE0400000000F2"
        )
        Handler(Looper.getMainLooper()).postDelayed({
            serialPort.sendData(
                "EE1000000000FE"
            )
        }, 200)
        Handler(Looper.getMainLooper()).postDelayed({
            if (!(textCur.text as String == "0 mA" || textCur.text as String == "0.00 mA")) {
                CurZero(serialPort, textCur)
                Log.d("demo", textCur.text as String)
            }
        }, 300)

    }

    // 定时器处理
    private inner class MyCountDownTimer(
        millisInFuture: Long,
        countDownInterval: Long,
        internal var tv: TextView,
        internal var serialPort: SerialPort
    ) : CountDownTimer(millisInFuture, countDownInterval) {
        override fun onFinish() {
            serialPort.sendData(
                "EE0200000000F0"
            )
            tv.setText("00:00")
        }

        override fun onTick(millisUntilFinished: Long) {
            tv.setText("${millisUntilFinished / 60000}:${millisUntilFinished / 1000 % 60}")
        }
    }
}

