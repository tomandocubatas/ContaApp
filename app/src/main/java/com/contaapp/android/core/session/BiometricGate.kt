package com.contaapp.android.core.session

import android.app.Activity
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal

class BiometricGate(private val activity: Activity) {
    fun authenticate(
        onSuccess: () -> Unit,
        onCancelOrError: (String) -> Unit
    ) {
        runCatching {
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle("Desbloquear ContaApp")
                .setSubtitle("Confirma tu identidad para ver tus cuentas")
                .setDescription("Los datos financieros permanecen bloqueados hasta validar tu huella o biometria.")
                .setNegativeButton("Cancelar", activity.mainExecutor) { _, _ ->
                    onCancelOrError("Autenticacion cancelada")
                }
                .build()

            prompt.authenticate(
                CancellationSignal(),
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        onCancelOrError(errString?.toString() ?: "No se pudo autenticar")
                    }

                    override fun onAuthenticationFailed() {
                        onCancelOrError("Huella no reconocida")
                    }
                }
            )
        }.onFailure { throwable ->
            onCancelOrError(throwable.message ?: "Biometria no disponible")
        }
    }
}
