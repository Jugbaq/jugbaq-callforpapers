package com.jugbaq.cfp.notifications;

public final class EmailTemplates {

    private EmailTemplates() {}

    public static final String SUBMISSION_RECEIVED_SUBJECT = "Recibimos tu propuesta: {{title}}";

    public static final String SUBMISSION_RECEIVED_BODY =
            """
        <!DOCTYPE html>
        <html>
        <body style="font-family: system-ui, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px;">
          <h1 style="color: #E8622C;">¡Gracias, {{speakerName}}! 🎉</h1>
          <p>Recibimos tu propuesta <strong>"{{title}}"</strong> para <strong>{{eventName}}</strong>.</p>
          <p>Los organizadores la revisarán pronto y te notificaremos del resultado por email.</p>
          <p>Mientras tanto, puedes ver el estado de tus propuestas en tu panel:</p>
          <p><a href="{{baseUrl}}/t/jugbaq/my-submissions"
               style="display: inline-block; background: #E8622C; color: white;
                      padding: 12px 24px; text-decoration: none; border-radius: 6px;">
               Ver mis propuestas
          </a></p>
          <hr style="margin: 32px 0; border: none; border-top: 1px solid #eee;">
          <p style="color: #888; font-size: 12px;">
            JUGBAQ — Java User Group Barranquilla
          </p>
        </body>
        </html>
        """;

    public static final String SUBMISSION_NEW_SUBJECT = "Nueva propuesta: {{title}}";

    public static final String SUBMISSION_NEW_BODY =
            """
        <!DOCTYPE html>
        <html>
        <body style="font-family: system-ui, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px;">
          <h1>Nueva propuesta recibida</h1>
          <p>Hola {{organizerName}},</p>
          <p><strong>{{speakerName}}</strong> envió una nueva propuesta para <strong>{{eventName}}</strong>:</p>
          <div style="background: #f5f5f5; padding: 16px; border-radius: 6px; margin: 16px 0;">
            <h2 style="margin-top: 0;">{{title}}</h2>
          </div>
          <p><a href="{{baseUrl}}/t/jugbaq/admin/reviews"
               style="display: inline-block; background: #333; color: white;
                      padding: 12px 24px; text-decoration: none; border-radius: 6px;">
               Ir a revisión
          </a></p>
        </body>
        </html>
        """;

    public static final String SUBMISSION_ACCEPTED_SUBJECT = "🎉 ¡Tu propuesta fue aceptada!: {{title}}";

    public static final String SUBMISSION_ACCEPTED_BODY =
            """
    <!DOCTYPE html>
    <html>
    <body style="font-family: system-ui, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px;">
      <h1 style="color: #00b04f;">¡Felicidades, {{speakerName}}! 🎉</h1>
      <p>Tu propuesta <strong>"{{title}}"</strong> fue <strong>aceptada</strong> para <strong>{{eventName}}</strong>.</p>
      <p>En los próximos días te enviaremos los detalles del evento, horario y logística.</p>
      <p><a href="{{baseUrl}}/t/jugbaq/my-submissions"
           style="display: inline-block; background: #00b04f; color: white;
                  padding: 12px 24px; text-decoration: none; border-radius: 6px;">
           Ver detalles
      </a></p>
      <hr style="margin: 32px 0; border: none; border-top: 1px solid #eee;">
      <p style="color: #888; font-size: 12px;">JUGBAQ — Java User Group Barranquilla</p>
    </body>
    </html>
    """;

    public static final String SUBMISSION_REJECTED_SUBJECT = "Sobre tu propuesta: {{title}}";

    public static final String SUBMISSION_REJECTED_BODY =
            """
    <!DOCTYPE html>
    <html>
    <body style="font-family: system-ui, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px;">
      <h1>Hola {{speakerName}}</h1>
      <p>Gracias por enviar tu propuesta <strong>"{{title}}"</strong> para <strong>{{eventName}}</strong>.</p>
      <p>Después de revisar todas las propuestas recibidas, esta vez no pudimos incluir la tuya en la agenda. Recibimos muchas propuestas de gran calidad y la selección fue difícil.</p>
      {{feedbackBlock}}
      <p>Te animamos a enviar nuevas propuestas para futuros eventos. ¡Gracias por ser parte de la comunidad!</p>
      <hr style="margin: 32px 0; border: none; border-top: 1px solid #eee;">
      <p style="color: #888; font-size: 12px;">JUGBAQ — Java User Group Barranquilla</p>
    </body>
    </html>
    """;
}
