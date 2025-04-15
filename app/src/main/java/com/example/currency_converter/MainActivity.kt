package com.example.currency_converter

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.currency_converter.adapter.AddCurrencyAdapter
import com.example.currency_converter.adapter.CurrencyAdapter
import com.example.currency_converter.databinding.ActivityMainBinding
import com.example.currency_converter.model.Calculator
import com.example.currency_converter.viewmodel.CurrencyViewModel
import com.google.android.material.button.MaterialButton
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.animation.AccelerateInterpolator
import android.view.ViewGroup
import android.graphics.Color
import android.widget.FrameLayout
import androidx.core.content.ContextCompat

/**
 * Главная активность приложения, которая отображает конвертер валют и калькулятор.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var dimOverlay: View
    private lateinit var binding: ActivityMainBinding
    private val viewModel: CurrencyViewModel by viewModels()
    private lateinit var currencyAdapter: CurrencyAdapter

    // Калькулятор использует функцию обратного вызова для обновления суммы активной валюты
    private val calculator = Calculator(onResultChanged = { result ->
        updateActiveCurrencyAmount(result)
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCurrencyRecyclerView()
        setupCalculatorButtons()
        setupUIComponents()
        observeViewModel()

        // Запрашиваем обновление курсов при старте
        viewModel.fetchLatestRates()

        // Создаем оверлей для затемнения
        setupDimOverlay()
    }

    /**
     * Настраивает затемнение фона
     */
    private fun setupDimOverlay() {
        // Добавляем оверлей для затемнения
        dimOverlay = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80000000")) // 50% прозрачный черный
            alpha = 0f
            visibility = View.GONE

            // Чтобы клики не проходили через затемнение
            setOnClickListener { /* пустой обработчик, чтобы перехватывать клики */ }
        }

        // Добавляем оверлей в корневой контейнер приложения
        (findViewById<View>(android.R.id.content) as ViewGroup).addView(dimOverlay)
    }

    /**
     * Настраивает RecyclerView для списка валют.
     */
    private fun setupCurrencyRecyclerView() {
        currencyAdapter = CurrencyAdapter { currency ->
            // Когда пользователь выбирает валюту, устанавливаем её как активную
            viewModel.setActiveCurrency(currency.code)

            // Визуально отмечаем активную валюту в адаптере
            currencyAdapter.setActiveCurrency(currency.code)

            // Обновляем дисплей калькулятора текущим значением валюты
            calculator.setDisplay(currency.amount.toString())

            // Показываем пользователю, какая валюта стала активной
            Toast.makeText(
                this,
                getString(R.string.active_currency_message, currency.code),
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.currencyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = currencyAdapter
            // Добавляем анимацию при изменении элементов
            itemAnimator?.changeDuration = 150
        }
    }

    /**
     * Обновляет сумму для активной валюты через ViewModel.
     * @param amount Новая сумма.
     */
    private fun updateActiveCurrencyAmount(amount: Double) {
        Timber.d("Updating active currency amount: $amount")
        viewModel.updateCurrencyAmount(amount)
    }

    /**
     * Настраивает кнопки калькулятора.
     */
    private fun setupCalculatorButtons() {
        with(binding) {
            // Цифровые кнопки
            button0.setOnClickListener { calculator.appendDigit("0") }
            button1.setOnClickListener { calculator.appendDigit("1") }
            button2.setOnClickListener { calculator.appendDigit("2") }
            button3.setOnClickListener { calculator.appendDigit("3") }
            button4.setOnClickListener { calculator.appendDigit("4") }
            button5.setOnClickListener { calculator.appendDigit("5") }
            button6.setOnClickListener { calculator.appendDigit("6") }
            button7.setOnClickListener { calculator.appendDigit("7") }
            button8.setOnClickListener { calculator.appendDigit("8") }
            button9.setOnClickListener { calculator.appendDigit("9") }
            buttonDot.setOnClickListener { calculator.appendDot() }

            // Кнопки операций
            buttonPlus.setOnClickListener { calculator.setOperation("+") }
            buttonMinus.setOnClickListener { calculator.setOperation("-") }
            buttonMultiply.setOnClickListener { calculator.setOperation("×") }
            buttonDivide.setOnClickListener { calculator.setOperation("÷") }
            buttonPercent.setOnClickListener { calculator.calculatePercent() }
            buttonEquals.setOnClickListener { calculator.calculateResult() }

            // Кнопки очистки
            buttonAC.setOnClickListener { calculator.clearAll() }
            buttonClear.setOnClickListener { calculator.clearLastDigit() }
        }
    }

    /**
     * Настраивает дополнительные элементы интерфейса.
     */
    private fun setupUIComponents() {
        // Кнопка редактирования списка валют
        binding.editButton.setOnClickListener {
            showAddCurrencyDialog()
        }

        // Кнопка настроек
        binding.settingsButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.settings_coming_soon), Toast.LENGTH_SHORT).show()
        }

        // Обновляем текст последнего обновления
        updateLastUpdateText()
    }

    /**
     * Обновляет текст времени последнего обновления курсов.
     */
    private fun updateLastUpdateText() {
        val dateFormat = SimpleDateFormat(getString(R.string.date_format), Locale.getDefault())
        val date = Date(viewModel.lastUpdateTime.value ?: System.currentTimeMillis())
        binding.lastUpdateTextView.text = getString(R.string.last_update_format, dateFormat.format(date))
    }

    /**
     * Настраивает наблюдателей LiveData из ViewModel.
     */
    private fun observeViewModel() {
        // Наблюдаем за списком выбранных валют
        viewModel.selectedCurrencies.observe(this) { currencies ->
            Timber.d("Received updated currencies: ${currencies.map { "${it.code}=${it.amount}" }}")
            currencyAdapter.submitList(currencies)

            // Если есть активная валюта, отмечаем её в адаптере
            viewModel.getActiveCurrencyCode().let { activeCode ->
                if (activeCode.isNotEmpty()) {
                    currencyAdapter.setActiveCurrency(activeCode)
                }
            }
        }

        // Наблюдаем за состоянием загрузки
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Наблюдаем за ошибками
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.takeIf { it.isNotEmpty() }?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Наблюдаем за временем последнего обновления курсов
        viewModel.lastUpdateTime.observe(this) { timestamp ->
            if (timestamp > 0) {
                updateLastUpdateText()
            }
        }
    }

    /**
     * Показывает диалог добавления/удаления валют.
     */
    private fun showAddCurrencyDialog() {
        // Показываем затемнение с анимацией
        dimOverlay.visibility = View.VISIBLE
        dimOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(AccelerateInterpolator())
            .start()

        val dialog = createAddCurrencyDialog()
        setupAddCurrencyDialogViews(dialog)

        // Добавляем обработчик закрытия диалога
        dialog.setOnDismissListener {
            hideAddCurrencyDialog()
        }

        dialog.show()
    }

    /**
     * Скрывает затемнение после закрытия диалога
     */
    private fun hideAddCurrencyDialog() {
        // Мгновенно скрываем затемнение
        dimOverlay.visibility = View.GONE
        dimOverlay.alpha = 0f
    }

    /**
     * Создает диалог добавления/удаления валют.
     * @return Созданный диалог.
     */
    private fun createAddCurrencyDialog(): Dialog {
        val dialog = Dialog(this, R.style.CurrencyDialogTheme)
        dialog.setContentView(R.layout.dialog_add_currency)

        // Настраиваем параметры окна
        dialog.window?.apply {
            // Настраиваем размеры диалога
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.9).toInt() // 90% ширины экрана

            setLayout(
                width,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            // Убираем стандартное затемнение, так как у нас своё
            setDimAmount(0f)

            // Центрируем диалог
            setGravity(android.view.Gravity.CENTER)

            // Добавляем анимацию
            attributes.windowAnimations = R.style.DialogAnimation

            // Устанавливаем прозрачный фон окна (чтобы видны были только наши стили)
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        return dialog
    }

    /**
     * Настраивает элементы интерфейса в диалоге добавления валют.
     * @param dialog Диалог для настройки.
     */
    private fun setupAddCurrencyDialogViews(dialog: Dialog) {
        val recyclerView: RecyclerView = dialog.findViewById(R.id.addCurrencyRecyclerView)
        val searchView: SearchView = dialog.findViewById(R.id.searchView)
        val confirmButton: MaterialButton = dialog.findViewById(R.id.buttonConfirm)

        setupAddCurrencyRecyclerView(recyclerView)
        setupAddCurrencySearch(searchView, recyclerView.adapter as AddCurrencyAdapter)

        confirmButton.setOnClickListener {
            // Обновляем курсы после изменения списка валют
            viewModel.fetchLatestRates()
            dialog.dismiss()
        }
    }

    /**
     * Настраивает RecyclerView для списка всех доступных валют.
     * @param recyclerView RecyclerView для настройки.
     */
    private fun setupAddCurrencyRecyclerView(recyclerView: RecyclerView) {
        val allCurrencies = viewModel.availableCurrencies.value.orEmpty()

        val adapter = AddCurrencyAdapter { currency, isChecked ->
            if (isChecked) {
                viewModel.addCurrency(currency.code)
            } else {
                // Проверяем, не удаляет ли пользователь активную валюту
                if (currency.code == viewModel.getActiveCurrencyCode()) {
                    Toast.makeText(
                        this,
                        getString(R.string.removing_active_currency),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                viewModel.removeCurrency(currency.code)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Обновляем состояние выбора в соответствии с текущим выбором
        val updatedCurrencies = allCurrencies.map { currency ->
            currency.copy(isSelected = viewModel.isCurrencySelected(currency.code))
        }
        adapter.submitList(updatedCurrencies)
    }

    /**
     * Настраивает поиск в диалоге добавления валют.
     * @param searchView View для поиска.
     * @param adapter Адаптер для фильтрации.
     */
    private fun setupAddCurrencySearch(
        searchView: SearchView,
        adapter: AddCurrencyAdapter
    ) {
        val allCurrencies = viewModel.availableCurrencies.value.orEmpty().map { currency ->
            currency.copy(isSelected = viewModel.isCurrencySelected(currency.code))
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    adapter.submitList(allCurrencies)
                } else {
                    val filteredList = allCurrencies.filter { currency ->
                        currency.code.contains(newText, ignoreCase = true) ||
                                currency.name.contains(newText, ignoreCase = true)
                    }
                    adapter.submitList(filteredList)
                }
                return true
            }
        })
    }
}