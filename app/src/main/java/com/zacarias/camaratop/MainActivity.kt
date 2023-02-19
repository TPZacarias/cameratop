package com.zacarias.camaratop

import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.zacarias.camaratop.databinding.ActivityMainBinding
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var ligacaoCamara: ActivityMainBinding
    private var capturaImagem: ImageCapture? = null
    private lateinit var diretorioDeSaida: File
    val telaRecebeImagem = TelaRecebeImagem()
    var data:String = String()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(Constantes.TAG, "onCreate")

        ligacaoCamara = ActivityMainBinding.inflate(layoutInflater)

        setContentView(ligacaoCamara.root)

        checaPermissoes()

        setListeners()

        setData()

        diretorioDeSaida = pegarDiretorioDeSaida()
    }

    private fun checaPermissoes() {
        Log.d(Constantes.TAG, "checaPermissoes")

        if (todasPermissoesConcedidas()) {
            criarCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                Constantes.REQUISICAO_PERMISSAO,
                Constantes.CODE_REQUISICAO_PERMISSAO
            )
        }
    }

    private fun setListeners() {
        Log.d(Constantes.TAG, "setListeners")

        ligacaoCamara.apply {
            botaoCamera.setOnClickListener {
                tirarFoto()
            }

        }

    }

    private fun setData() {
        Log.d(Constantes.TAG, "setData")

        val dataFormatada: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        val captarHoraMain: LocalDateTime = LocalDateTime.now()

        data = dataFormatada.format(captarHoraMain).toString()
    }

    private fun pegarDiretorioDeSaida(): File {
        Log.d(Constantes.TAG, "pegarDiretorioDeSaida")

        val diretorioDeMidea = externalMediaDirs.firstOrNull().let { meuArquivo ->
            File(meuArquivo, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }

        return if (diretorioDeMidea.exists())
            diretorioDeMidea
        else
            filesDir
    }

    private fun tirarFoto() {
        Log.d(Constantes.TAG, "tirarFoto")

        // Retorna uma instancia do Camera X
        val capturaFoto = capturaImagem ?: return

        // Cria o arquivo para armazenar a foto
        val arquivoFoto = File(
            diretorioDeSaida,
            SimpleDateFormat(
                Constantes.FORMATO_NOME_ARQUIVO,
                Locale.getDefault()
            )
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val opcaoSaida = ImageCapture
            .OutputFileOptions
            .Builder(arquivoFoto)
            .build()

        capturaFoto.takePicture(
            opcaoSaida,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d(Constantes.TAG, "onImageSaved")

                    ligacaoCamara.vistaReduzida.setImageURI(Uri.fromFile(arquivoFoto))

                    ligacaoCamara.apply {
                        vistaReduzida.setOnClickListener {
                            abrirSegundatela()
                        }
                    }
                    }

                private fun abrirSegundatela() {

                    val intencaoArquivo = Intent(this@MainActivity,TelaRecebeImagem::class.java)
                    intencaoArquivo.putExtra("receber",arquivoFoto.toString())
                    intencaoArquivo.putExtra("data",data)
                    intencaoArquivo.putExtra("formato",Constantes.FORMATO_NOME_ARQUIVO)
                    startActivity(intencaoArquivo)

                    Toast.makeText(
                        this@MainActivity,
                        " IMAGEM SALVA! ${Uri.fromFile(arquivoFoto)}",
                        Toast.LENGTH_LONG).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constantes.TAG, "onError: ${exception.message}", exception)
                }
            }
        )
    }

    private fun criarCamera() {
        Log.d(Constantes.TAG, "criarCamera")

        val provedorFuturoCamera = ProcessCameraProvider.getInstance(this)

        provedorFuturoCamera.addListener({
            val provedorCamera: ProcessCameraProvider = provedorFuturoCamera.get()

            val visualizacao = Preview.Builder().build()
                .also { visualizacaoLambda ->
                    visualizacaoLambda.setSurfaceProvider(ligacaoCamara.cameraVista.surfaceProvider)
                }

            capturaImagem = ImageCapture.Builder().build()

            val seletorDeCamera = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                provedorCamera.unbindAll()
                provedorCamera.bindToLifecycle(
                    this, seletorDeCamera, visualizacao, capturaImagem
                )
            } catch (e: Exception) {
                Log.d(Constantes.TAG, "A CÂMERA NÃO LIGOU!", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(Constantes.TAG, "onRequestPermissionsResult")

        if (requestCode == Constantes.CODE_REQUISICAO_PERMISSAO) {
            if (todasPermissoesConcedidas()) {
                criarCamera()
            } else {
                Toast.makeText(this, R.string.permissao_negada, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun todasPermissoesConcedidas() =
        Constantes.REQUISICAO_PERMISSAO.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }
}
