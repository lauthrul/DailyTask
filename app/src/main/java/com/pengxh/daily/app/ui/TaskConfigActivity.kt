package com.pengxh.daily.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import com.pengxh.daily.app.R
import com.pengxh.daily.app.databinding.ActivityTaskConfigBinding
import com.pengxh.daily.app.extensions.initImmersionBar
import com.pengxh.daily.app.service.FloatingWindowService
import com.pengxh.daily.app.utils.Constant
import com.pengxh.daily.app.utils.DatabaseWrapper
import com.pengxh.daily.app.widgets.TaskMessageDialog
import com.pengxh.kt.lite.base.KotlinBaseActivity
import com.pengxh.kt.lite.extensions.convertColor
import com.pengxh.kt.lite.extensions.isNumber
import com.pengxh.kt.lite.extensions.show
import com.pengxh.kt.lite.utils.SaveKeyValues
import com.pengxh.kt.lite.widget.TitleBarView
import com.pengxh.kt.lite.widget.dialog.AlertInputDialog
import com.pengxh.kt.lite.widget.dialog.BottomActionSheet

class TaskConfigActivity : KotlinBaseActivity<ActivityTaskConfigBinding>() {

    private val kTag = "DailyTask.TaskConfigActivity"
    private val context = this
    private val hourArray = arrayListOf("0", "1", "2", "3", "4", "5", "6", "自定义（单位：时）")
    private val timeArray = arrayListOf("15s", "30s", "45s", "自定义（单位：秒）")
    private val clipboard by lazy { getSystemService(CLIPBOARD_SERVICE) as ClipboardManager }

    override fun initEvent() {
        binding.resetTimeLayout.setOnClickListener {
            BottomActionSheet.Builder().setContext(this).setActionItemTitle(hourArray)
                .setItemTextColor(R.color.theme_color.convertColor(this))
                .setOnActionSheetListener(object : BottomActionSheet.OnActionSheetListener {
                    override fun onActionItemClick(position: Int) {
                        setHourByPosition(position)
                    }
                }).build().show()
        }

        binding.timeoutLayout.setOnClickListener {
            BottomActionSheet.Builder().setContext(this).setActionItemTitle(timeArray)
                .setItemTextColor(R.color.theme_color.convertColor(this))
                .setOnActionSheetListener(object : BottomActionSheet.OnActionSheetListener {
                    override fun onActionItemClick(position: Int) {
                        setTimeByPosition(position)
                    }
                }).build().show()
        }

        binding.keyLayout.setOnClickListener {
            AlertInputDialog.Builder().setContext(this).setTitle("设置打卡口令")
                .setHintMessage("请输入打卡口令，如：打卡").setNegativeButton("取消")
                .setPositiveButton("确定").setOnDialogButtonClickListener(object :
                    AlertInputDialog.OnDialogButtonClickListener {
                    override fun onConfirmClick(value: String) {
                        SaveKeyValues.putValue(Constant.TASK_NAME_KEY, value)
                        binding.keyTextView.text = value
                    }

                    override fun onCancelClick() {}
                }).build().show()
        }

        binding.randomTimeSwitch.setOnCheckedChangeListener { _, isChecked ->
            SaveKeyValues.putValue(Constant.RANDOM_TIME_KEY, isChecked)
        }

        binding.outputLayout.setOnClickListener {
            val taskBeans = DatabaseWrapper.loadAllTask()

            if (taskBeans.isEmpty()) {
                "没有任务可以导出".show(this)
                return@setOnClickListener
            }

            TaskMessageDialog.Builder().setContext(this).setTasks(taskBeans)
                .setOnDialogButtonClickListener(object :
                    TaskMessageDialog.OnDialogButtonClickListener {
                    override fun onConfirmClick(taskValue: String) {
                        val cipData = ClipData.newPlainText("DailyTask", taskValue)
                        clipboard.setPrimaryClip(cipData)
                        "任务已复制到剪切板".show(context)
                    }
                }).build().show()
        }
    }

    private fun setHourByPosition(position: Int) {
        if (position == hourArray.size - 1) {
            AlertInputDialog.Builder().setContext(this).setTitle("设置重置时间")
                .setHintMessage("直接输入整数时间即可，如：6").setNegativeButton("取消")
                .setPositiveButton("确定").setOnDialogButtonClickListener(object :
                    AlertInputDialog.OnDialogButtonClickListener {
                    override fun onConfirmClick(value: String) {
                        if (value.isNumber()) {
                            binding.resetTimeView.text = "每天${value}点"
                            SaveKeyValues.putValue(Constant.RESET_TIME_KEY, value.toInt())
                        } else {
                            "直接输入整数时间即可".show(context)
                        }
                    }

                    override fun onCancelClick() {}
                }).build().show()
        } else {
            val hour = hourArray[position]
            binding.resetTimeView.text = "每天${hour}点"
            SaveKeyValues.putValue(Constant.RESET_TIME_KEY, hour.toInt())
        }
    }

    private fun setTimeByPosition(position: Int) {
        if (position == timeArray.size - 1) {
            AlertInputDialog.Builder().setContext(this).setTitle("设置超时时间")
                .setHintMessage("直接输入整数时间即可，如：60").setNegativeButton("取消")
                .setPositiveButton("确定").setOnDialogButtonClickListener(object :
                    AlertInputDialog.OnDialogButtonClickListener {
                    override fun onConfirmClick(value: String) {
                        if (value.isNumber()) {
                            val time = "${value}s"
                            binding.timeoutTextView.text = time
                            SaveKeyValues.putValue(Constant.STAY_DD_TIMEOUT_KEY, time)
                            FloatingWindowService.weakReferenceHandler?.let {
                                val message = it.obtainMessage()
                                message.what = Constant.UPDATE_TICK_TIME_CODE
                                message.obj = time
                                it.sendMessage(message)
                            }
                        } else {
                            "直接输入整数时间即可".show(context)
                        }
                    }

                    override fun onCancelClick() {}
                }).build().show()
        } else {
            val time = timeArray[position]
            binding.timeoutTextView.text = time
            SaveKeyValues.putValue(Constant.STAY_DD_TIMEOUT_KEY, time)
            FloatingWindowService.weakReferenceHandler?.let {
                val message = it.obtainMessage()
                message.what = Constant.UPDATE_TICK_TIME_CODE
                message.obj = time
                it.sendMessage(message)
            }
        }
    }

    override fun initOnCreate(savedInstanceState: Bundle?) {
        val hour = SaveKeyValues.getValue(
            Constant.RESET_TIME_KEY, Constant.DEFAULT_RESET_HOUR
        ) as Int
        binding.resetTimeView.text = "每天${hour}点"
        binding.timeoutTextView.text = SaveKeyValues.getValue(
            Constant.STAY_DD_TIMEOUT_KEY, Constant.DEFAULT_OVER_TIME
        ) as String
        binding.keyTextView.text = SaveKeyValues.getValue(Constant.TASK_NAME_KEY, "打卡") as String
        val needRandom = SaveKeyValues.getValue(Constant.RANDOM_TIME_KEY, true) as Boolean
        binding.randomTimeSwitch.isChecked = needRandom
    }

    override fun initViewBinding(): ActivityTaskConfigBinding {
        return ActivityTaskConfigBinding.inflate(layoutInflater)
    }

    override fun observeRequestState() {

    }

    override fun setupTopBarLayout() {
        binding.rootView.initImmersionBar(this, true, R.color.white)
        binding.titleView.setOnClickListener(object : TitleBarView.OnClickListener {
            override fun onLeftClick() {
                finish()
            }

            override fun onRightClick() {

            }
        })
    }
}